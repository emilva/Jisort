/*
 * Copyright (C) 2013-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sufficientlysecure.htmltextview;

import android.content.Context;
import android.text.Html;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import java.io.InputStream;
import java.util.Scanner;

public class HtmlTextView extends JellyBeanSpanFixTextView {

    public static final String TAG = "HtmlTextView";
    public static final boolean DEBUG = false;
    boolean mDontConsumeNonUrlClicks = true;
    boolean mLinkHit;
    private boolean mfunMode = false;
    private boolean removeFromHtmlSpace = false;
    private ClickableTableSpan mClickableTableSpan;
    private DrawTableLinkSpan mDrawTableLinkSpan;

    public HtmlTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public HtmlTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HtmlTextView(Context context) {
        super(context);
    }

    /**
     * Note that this must be called before setting text for it to work
     */
    public void setRemoveFromHtmlSpace(boolean removeFromHtmlSpace) {
        this.removeFromHtmlSpace = removeFromHtmlSpace;
    }

    /**
     * Note that this must be called before setting text for it to work
     * @param funMode true/false
     */
    public void setFunMode(boolean funMode) {
        this.mfunMode = funMode;
    }

    public interface ImageGetter {
    }

    public static class LocalImageGetter implements ImageGetter {
    }

    public static class RemoteImageGetter implements ImageGetter {
        public String baseUrl;

        public RemoteImageGetter() {
        }

        public RemoteImageGetter(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }

    /**
     * http://stackoverflow.com/questions/309424/read-convert-an-inputstream-to-a-string
     */
    static private String convertStreamToString(InputStream is) {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mLinkHit = false;
        boolean res = super.onTouchEvent(event);

        if (mDontConsumeNonUrlClicks) {
            return mLinkHit;
        }
        return res;
    }

    /**
     * Loads HTML from a raw resource, i.e., a HTML file in res/raw/.
     * This allows translatable resource (e.g., res/raw-de/ for german).
     * The containing HTML is parsed to Android's Spannable format and then displayed.
     *
     * @param context Context
     * @param resId   for example: R.raw.help
     */
    public void setHtmlFromRawResource(Context context, int resId, ImageGetter imageGetter) {
        // load html from html file from /res/raw
        InputStream inputStreamText = context.getResources().openRawResource(resId);

        setHtmlFromString(convertStreamToString(inputStreamText), imageGetter);
    }

    /**
     * Parses String containing HTML to Android's Spannable format and displays it in this TextView.
     *
     * @param html String containing HTML, for example: "<b>Hello world!</b>"
     */
    public void setHtmlFromString(String html, ImageGetter imageGetter) {
        Html.ImageGetter htmlImageGetter;
        if (imageGetter instanceof LocalImageGetter) {
            htmlImageGetter = new HtmlLocalImageGetter(getContext());
        } else if (imageGetter instanceof RemoteImageGetter) {
            htmlImageGetter = new HtmlRemoteImageGetter(this,
                    ((RemoteImageGetter) imageGetter).baseUrl);
        } else {
            Log.e(TAG, "Wrong imageGetter!");
            return;
        }
        setHtmlFromStringWithHtmlImageGetter(html, htmlImageGetter);
    }

    /**
     * Parses String containing HTML to Android's Spannable format and displays it in this TextView.
     * Using the implementation of Html.ImageGetter provided. This allows using external libraries
     * for fetching and caching images.
     *
     * @param html String containing HTML, for example: "<b>Hello world!</b>"
     * @param htmlImageGetter for fetching images
     */
    public void setHtmlFromStringWithHtmlImageGetter(String html, Html.ImageGetter htmlImageGetter) {
        // this uses Android's Html class for basic parsing, and HtmlTagHandler
        final HtmlTagHandler htmlTagHandler = new HtmlTagHandler();
        htmlTagHandler.setClickableTableSpan(mClickableTableSpan);
        htmlTagHandler.setDrawTableLinkSpan(mDrawTableLinkSpan);

        if(mfunMode){
            html = EmojiUtils.parse(html);
        }

        if (removeFromHtmlSpace) {
            setText(removeHtmlBottomPadding(Html.fromHtml(html, htmlImageGetter, htmlTagHandler)));
        } else {
            setText(Html.fromHtml(html, htmlImageGetter, htmlTagHandler));
        }

        // make links work
        setMovementMethod(LocalLinkMovementMethod.getInstance());
    }

    /**
     * @deprecated
     */
    public void setHtmlFromRawResource(Context context, int resId, boolean useLocalDrawables) {
        if (useLocalDrawables) {
            setHtmlFromRawResource(context, resId, new LocalImageGetter());
        } else {
            setHtmlFromRawResource(context, resId, new RemoteImageGetter());
        }
    }

    /**
     * @deprecated
     */
    public void setHtmlFromString(String html, boolean useLocalDrawables) {
        if (useLocalDrawables) {
            setHtmlFromString(html, new LocalImageGetter());
        } else {
            setHtmlFromString(html, new RemoteImageGetter());
        }
    }

    public void setClickableTableSpan(ClickableTableSpan clickableTableSpan) {
        this.mClickableTableSpan = clickableTableSpan;
    }

    public void setDrawTableLinkSpan(DrawTableLinkSpan drawTableLinkSpan) {
        this.mDrawTableLinkSpan = drawTableLinkSpan;
    }

    private CharSequence removeHtmlBottomPadding(CharSequence text) {
        if (text == null) {
            return null;
        } else if (text.length() == 0) {
            return text;
        }

        while (text.charAt(text.length() - 1) == '\n') {
            text = text.subSequence(0, text.length() - 1);
        }
        return text;
    }
}
