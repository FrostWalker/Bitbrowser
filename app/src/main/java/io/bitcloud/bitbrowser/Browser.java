package io.bitcloud.bitbrowser;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebIconDatabase;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashSet;

public class Browser extends AppCompatActivity {

    private WebView webView;
    private AutoCompleteTextView urlBar;
    private LinearLayout toolBar;
    private ProgressBar progressBar;
    private SharedPreferences prefs;

    private HistoryManager historyManager;
    private BookmarkManager bookmarkManager;

    private InterfaceWindow optionsMenu, bookmarks, history, settings, help, searchSelect;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        WebIconDatabase.getInstance().open(getDir("icons", MODE_PRIVATE).getPath());
        setContentView(R.layout.browser);
        this.prefs = getSharedPreferences("io.bitcloud.bitbrowser", MODE_PRIVATE);
        historyManager = new HistoryManager(this);
        bookmarkManager = new BookmarkManager(this);

        RelativeLayout browser = (RelativeLayout) findViewById(R.id.browser);
        webView = (WebView) findViewById(R.id.webview);
        urlBar = (AutoCompleteTextView)findViewById(R.id.toolbar_urlbar);
        toolBar = (LinearLayout)findViewById(R.id.toolbar);
        progressBar = (ProgressBar)findViewById(R.id.progressbar);

        this.optionsMenu = new InterfaceWindow(this, browser, R.layout.popup_menu, LinearLayout.LayoutParams.MATCH_PARENT, R.style.options_anim);
        this.bookmarks = new InterfaceWindow(this, browser, R.layout.bookmarks, LinearLayout.LayoutParams.MATCH_PARENT, R.style.window_anim);
        this.history = new InterfaceWindow(this, browser, R.layout.history, LinearLayout.LayoutParams.MATCH_PARENT, R.style.window_anim);
        this.settings = new InterfaceWindow(this, browser, R.layout.settings, LinearLayout.LayoutParams.MATCH_PARENT, R.style.window_anim);
        this.help = new InterfaceWindow(this, browser, R.layout.help, LinearLayout.LayoutParams.MATCH_PARENT, R.style.window_anim);
        this.searchSelect = new InterfaceWindow(this, browser, R.layout.search_select, LinearLayout.LayoutParams.MATCH_PARENT, R.style.options_anim);

        urlBar.setAdapter(historyManager.getAdapter());
        urlBar.setThreshold(1);

        initiate();
        webView.bringToFront();

        final EditText settingsHomepage = (EditText)this.settings.findViewById(R.id.settings_homepage);
        settingsHomepage.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN) && (i == KeyEvent.KEYCODE_ENTER)) {
                    String url = settingsHomepage.getText().toString();
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(settingsHomepage.getWindowToken(), 0);
                    settingsHomepage.clearFocus();

                    setHomepage(url);
                    return true;
                }
                return false;
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                showInterface();
                updateNavButtons();
                updateBookmarkButtons();
                urlBar.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                urlBar.setText(url);
                urlBar.clearFocus();
                webView.requestFocus();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                updateNavButtons();
                updateBookmarkButtons();

                Bitmap favicon = view.getFavicon();
                if(favicon == null) {
                    urlBar.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                } else {
                    favicon = Bitmap.createScaledBitmap(favicon, dp(24), dp(24), false);
                    urlBar.setCompoundDrawablesWithIntrinsicBounds(new BitmapDrawable(getResources(), favicon), null, null, null);
                }

                if(!getFlag("private_mode") && !webView.getTitle().equals(""))
                    historyManager.add(url, view.getTitle(), view.getFavicon());
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onReceivedIcon(WebView view, Bitmap favicon) {
                super.onReceivedIcon(view, favicon);
                favicon = Bitmap.createScaledBitmap(favicon, dp(24), dp(24), false);
                urlBar.setCompoundDrawablesWithIntrinsicBounds(new BitmapDrawable(getResources(), favicon), null, null, null);

                historyManager.updateFavicon(view.getUrl(), favicon);
                bookmarkManager.updateFavicon(view.getUrl(), favicon);
            }

            @Override
            public void onProgressChanged(WebView view, int progress) {
                ImageView tr = (ImageView)findViewById(R.id.toolbar_refresh);
                ImageView ts = (ImageView)findViewById(R.id.toolbar_stop);
                ImageView mr = (ImageView)optionsMenu.findViewById(R.id.menu_control_refresh);
                ImageView ms = (ImageView)optionsMenu.findViewById(R.id.menu_control_stop);

                if(progress < 100) {
                    if (isTablet()) {
                        ts.setVisibility(View.VISIBLE);
                        tr.setVisibility(View.GONE);
                    } else {
                        ms.setVisibility(View.VISIBLE);
                        mr.setVisibility(View.GONE);
                    }

                    progressBar.setBackgroundColor(colour(getFlag("private_mode") ? R.color.progressFocusBackgroundDark : R.color.progressFocusBackgroundLight));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        progressBar.setProgress(progress, true);
                    else
                        progressBar.setProgress(progress);
                } else {
                    if (isTablet()) {
                        tr.setVisibility(View.VISIBLE);
                        ts.setVisibility(View.GONE);
                    } else {
                        mr.setVisibility(View.VISIBLE);
                        ms.setVisibility(View.GONE);
                    }
                    progressBar.setBackgroundColor(colour(getFlag("private_mode") ? R.color.progressBackgroundDark : R.color.progressBackgroundLight));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        progressBar.setProgress(0, true);
                    else
                        progressBar.setProgress(0);
                }
            }
        });

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null && type.equals("text/plain"))
                webView.loadUrl(intent.getStringExtra(Intent.EXTRA_TEXT));
        else if (state != null)
            webView.restoreState(state);
        else
            this.loadHome(null);

        urlBar.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                GradientDrawable drawable = (GradientDrawable)findViewById(R.id.toolbar_urlbar).getBackground();
                if(b) {
                    urlBar.selectAll();
                    drawable.setColor(colour(getFlag("private_mode") ? R.color.urlbarFocusBackgroundDark : R.color.urlbarFocusBackgroundLight));
                    urlBar.setTextColor(Color.rgb(0, 0, 0));

                } else {
                    urlBar.setText(webView.getUrl());
                    drawable.setColor(colour(getFlag("private_mode") ? R.color.urlbarBackgroundDark : R.color.urlbarBackgroundLight));
                    urlBar.setTextColor(Color.rgb(25, 25, 25));
                }
            }
        });

        urlBar.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN) && (i == KeyEvent.KEYCODE_ENTER)) {
                    String url = urlBar.getText().toString();
                    if(url.contains("://"))
                        webView.loadUrl(url);
                    else if(!url.contains(" ") && url.contains("."))
                        webView.loadUrl("http://" + url);
                    else {
                        String prov = getParam("search_provider");
                        switch (prov) {
                            case "Google":
                                webView.loadUrl("https://www.google.com/search?q=" + url);
                                break;
                            case "Bing":
                                webView.loadUrl("https://www.bing.com/search?q=" + url);
                                break;
                            case "Yahoo":
                                webView.loadUrl("https://search.yahoo.co.jp/search?p=" + url);
                                break;
                            case "Baidu":
                                webView.loadUrl("https://www.baidu.com/s?wd=" + url);
                                break;
                            default:
                                webView.loadUrl("https://start.duckduckgo.com/?q=" + url);
                                break;
                        }
                    }
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(urlBar.getWindowToken(), 0);
                    urlBar.clearFocus();
                    webView.requestFocus();

                    webView.setBackgroundColor(Color.TRANSPARENT);
                    webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
                    return true;
                }
                return false;
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            webView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                int[] last = new int[20];
                int pos = 0;
                @Override
                public void onScrollChange(View view, int i, int i1, int i2, int i3) {
                    int sum = 0;
                    for (int n : last) sum += n;
                    int avg = sum/20;
                    last[pos++] = i1;
                    pos %= 20;

                    if(i1 < dp(56) || i1+64 < avg) {
                        showInterface();

                    } else if(i1 >= 128 && i1-64 > avg && getFlag("toolbar_autohide")) {
                        hideInterface();
                    }

                }
            });
        }

        webView.requestFocus();
    }

    public void pageBack(View view) {
        if(webView.canGoBack()) {
            webView.goBack();
        }
    }

    public void pageForward(View view) {
        if(webView.canGoForward()) {
            webView.goForward();
        }
    }

    public void addBookmark(View view) {
        bookmarkManager.add(webView.getUrl(), webView.getTitle(), webView.getFavicon());
        updateBookmarkButtons();
    }

    public void removeBookmark(View view) {
        bookmarkManager.remove(webView.getUrl());
        updateBookmarkButtons();
    }

    public void loadHome(View view) {
        String url = getParam("homepage");
        if(url == null || url.equals(""))
            url = "about:blank";
        webView.loadUrl(url);
        optionsMenu.dismiss();
    }

    public void share(View view) {
        optionsMenu.dismiss();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setDataAndNormalize(Uri.parse(webView.getUrl()));
        intent.putExtra(Intent.EXTRA_TEXT, webView.getUrl());
        intent.setType("text/plain");
        startActivity(Intent.createChooser(intent,"Share"));
    }

    public void reloadPage(View view) {
        webView.loadUrl(webView.getUrl());
    }

    public void stopLoad(View view) {
        webView.stopLoading();
    }

    public void openMenu(View view) {
        optionsMenu.show();
    }

    public void closeMenu(View view) {
        optionsMenu.dismiss();
    }

    public void openBookmarks(View view) {
        optionsMenu.dismiss();
        bookmarks.show();

        ListView list = (ListView)bookmarks.findViewById(R.id.content_list);
        list.setAdapter(bookmarkManager.getAdapter());
    }

    public void closeBookmarks(View view) {
        bookmarks.dismiss();
    }

    public void openHistory(View view) {
        optionsMenu.dismiss();
        history.show();

        ListView list = (ListView)history.findViewById(R.id.content_list);
        list.setAdapter(historyManager.getAdapter());
    }

    public void closeHistory(View view) {
        history.dismiss();
    }

    public void openSettings(View view) {
        optionsMenu.dismiss();
        settings.show();
    }

    public void closeSettings(View view) {
        settings.dismiss();
    }

    public void openSearchSelect(View view) {
        searchSelect.show();
    }

    public void closeSearchSelect(View view) {
        searchSelect.dismiss();
    }


    public void openHelp(View view) {
        optionsMenu.dismiss();
        help.show();
    }

    public void closeHelp(View view) {
        help.dismiss();
    }

    public void showInterface() {
        toolBar.setVisibility(View.VISIBLE);
        progressBar.setBackgroundColor(colour(getFlag("private_mode") ? R.color.progressBackgroundDark : R.color.progressBackgroundLight));

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, dp(3));
        params.addRule(getFlag("urlbar_top") ? RelativeLayout.BELOW : RelativeLayout.ABOVE, R.id.toolbar);
        progressBar.setLayoutParams(params);
    }

    public void hideInterface() {
        toolBar.setVisibility(View.GONE);
        progressBar.setBackgroundColor(colour(R.color.progressBackgroundDark));

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, dp(3));
        params.addRule(getFlag("urlbar_top") ? RelativeLayout.ALIGN_PARENT_TOP : RelativeLayout.ALIGN_PARENT_BOTTOM);
        progressBar.setLayoutParams(params);
    }

    public void clearHistory(View view) {
        historyManager.clearRecords();
    }

    public void updateBookmarkButtons() {
        ImageView a = (ImageView)optionsMenu.findViewById(R.id.menu_tools_addbookmark);
        ImageView r = (ImageView)optionsMenu.findViewById(R.id.menu_tools_removebookmark);

        if(bookmarkManager.contains(webView.getUrl())) {
            a.setVisibility(View.GONE);
            r.setVisibility(View.VISIBLE);
        } else {
            a.setVisibility(View.VISIBLE);
            r.setVisibility(View.GONE);
        }
    }

    public void updateNavButtons() {
        ImageView toolBck = (ImageView)findViewById(R.id.toolbar_back);
        ImageView toolFwd = (ImageView)findViewById(R.id.toolbar_forward);
        ImageView menuBck = (ImageView)optionsMenu.findViewById(R.id.menu_control_back);
        ImageView menuFwd = (ImageView)optionsMenu.findViewById(R.id.menu_control_forward);

        if(webView.canGoBack()) {
            toolBck.setImageAlpha(255);
            menuBck.setImageAlpha(255);
        } else {
            toolBck.setImageAlpha(100);
            menuBck.setImageAlpha(100);
        }

        if(webView.canGoForward()) {
            toolFwd.setImageAlpha(255);
            menuFwd.setImageAlpha(255);
        } else {
            toolFwd.setImageAlpha(100);
            menuFwd.setImageAlpha(100);
        }
    }

    public void toggleJavascript(View view) {
        Switch s = (Switch)view;
        setJavascript(s.isChecked());
    }

    public void setJavascript(boolean b) {
        Switch s = (Switch)settings.findViewById(R.id.settings_javascript);
        s.setChecked(b);
        webView.getSettings().setJavaScriptEnabled(b);
        setFlag("javascript_enabled", b);
    }

    public void togglePrivate(View view) {
        Switch s = (Switch)view;
        setPrivate(s.isChecked());
    }

    public void setPrivate(boolean b) {
        Switch s = (Switch)settings.findViewById(R.id.settings_private);
        s.setChecked(b);
        setFlag("private_mode", b);

        toolBar.setBackgroundColor(colour(b ? R.color.toolbarBackgroundDark : R.color.toolbarBackgroundLight));
        progressBar.setBackgroundColor(colour(b ? R.color.progressBackgroundDark : R.color.progressBackgroundLight));
        {
            GradientDrawable drawable = (GradientDrawable)optionsMenu.findViewById(R.id.options_menu).getBackground();
            drawable.setColor(colour(b ? R.color.popupBackgroundDark : R.color.popupBackgroundLight));
            drawable.setStroke(dp(1), colour(b ? R.color.popupBorderDark : R.color.popupBorderLight));
        }
        {
            GradientDrawable drawable = (GradientDrawable)findViewById(R.id.toolbar_urlbar).getBackground();
            drawable.setColor(colour(b ? R.color.urlbarBackgroundDark : R.color.urlbarBackgroundLight));
        }
        {
            ((ImageView)findViewById(R.id.toolbar_menu)).getDrawable().setTint(colour(b ? R.color.toolbarTextDark : R.color.toolbarTextLight));
            ((ImageView)findViewById(R.id.toolbar_back)).getDrawable().setTint(colour(b ? R.color.toolbarTextDark : R.color.toolbarTextLight));
            ((ImageView)findViewById(R.id.toolbar_forward)).getDrawable().setTint(colour(b ? R.color.toolbarTextDark : R.color.toolbarTextLight));
            ((ImageView)findViewById(R.id.toolbar_refresh)).getDrawable().setTint(colour(b ? R.color.toolbarTextDark : R.color.toolbarTextLight));
            ((ImageView)findViewById(R.id.toolbar_stop)).getDrawable().setTint(colour(b ? R.color.toolbarTextDark : R.color.toolbarTextLight));

            ((ImageView)optionsMenu.findViewById(R.id.menu_control_back)).getDrawable().setTint(colour(b ? R.color.toolbarTextDark : R.color.toolbarTextLight));
            ((ImageView)optionsMenu.findViewById(R.id.menu_control_forward)).getDrawable().setTint(colour(b ? R.color.toolbarTextDark : R.color.toolbarTextLight));
            ((ImageView)optionsMenu.findViewById(R.id.menu_control_refresh)).getDrawable().setTint(colour(b ? R.color.toolbarTextDark : R.color.toolbarTextLight));
            ((ImageView)optionsMenu.findViewById(R.id.menu_control_stop)).getDrawable().setTint(colour(b ? R.color.toolbarTextDark : R.color.toolbarTextLight));
            ((ImageView)optionsMenu.findViewById(R.id.menu_control_menu)).getDrawable().setTint(colour(b ? R.color.toolbarTextDark : R.color.toolbarTextLight));

            ((ImageView)optionsMenu.findViewById(R.id.menu_tools_addbookmark)).getDrawable().setTint(colour(b ? R.color.toolbarTextDark : R.color.toolbarTextLight));
            ((ImageView)optionsMenu.findViewById(R.id.menu_tools_removebookmark)).getDrawable().setTint(colour(b ? R.color.toolbarTextDark : R.color.toolbarTextLight));
            ((ImageView)optionsMenu.findViewById(R.id.menu_tools_home)).getDrawable().setTint(colour(b ? R.color.toolbarTextDark : R.color.toolbarTextLight));
            ((ImageView)optionsMenu.findViewById(R.id.menu_tools_share)).getDrawable().setTint(colour(b ? R.color.toolbarTextDark : R.color.toolbarTextLight));
            ((ImageView)optionsMenu.findViewById(R.id.menu_tools_menu)).getDrawable().setTint(colour(b ? R.color.toolbarTextDark : R.color.toolbarTextLight));

            ((TextView)optionsMenu.findViewById(R.id.menu_items_bookmarks)).setTextColor(colour(b ? R.color.toolbarTextDark : R.color.toolbarTextLight));
            ((TextView)optionsMenu.findViewById(R.id.menu_items_history)).setTextColor(colour(b ? R.color.toolbarTextDark : R.color.toolbarTextLight));
            ((TextView)optionsMenu.findViewById(R.id.menu_items_settings)).setTextColor(colour(b ? R.color.toolbarTextDark : R.color.toolbarTextLight));
            ((TextView)optionsMenu.findViewById(R.id.menu_items_help)).setTextColor(colour(b ? R.color.toolbarTextDark : R.color.toolbarTextLight));
        }


    }

    public void toggleUrlBar(View view) {
        Switch s = (Switch)view;
        setUrlBar(s.isChecked());
    }

    public void setUrlBar(boolean b) {
        Switch s = (Switch)settings.findViewById(R.id.settings_topbar);
        s.setChecked(b);

        urlBar.setDropDownVerticalOffset(b ? dp(5) : -dp(5));

        {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
            params.addRule(b ? RelativeLayout.ALIGN_PARENT_BOTTOM : RelativeLayout.ALIGN_PARENT_TOP);
            params.addRule(b ? RelativeLayout.BELOW : RelativeLayout.ABOVE, R.id.progressbar);
            webView.setLayoutParams(params);
        }
        {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, dp(3));
            params.addRule(b ? RelativeLayout.BELOW : RelativeLayout.ABOVE, R.id.toolbar);
            progressBar.setLayoutParams(params);
        }
        {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, dp(56));
            params.addRule(b ? RelativeLayout.ALIGN_PARENT_TOP : RelativeLayout.ALIGN_PARENT_BOTTOM);
            toolBar.setLayoutParams(params);

        }
        {
            {
                RelativeLayout options = (RelativeLayout)optionsMenu.findViewById(R.id.options_menu);
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                params.addRule(b ? RelativeLayout.ALIGN_PARENT_TOP : RelativeLayout.ALIGN_PARENT_BOTTOM);
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                options.setLayoutParams(params);
            }
            {
                LinearLayout optionItems = (LinearLayout)optionsMenu.findViewById(R.id.menu_items);
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                if(isTablet()) {
                    if(b)
                        params.addRule(RelativeLayout.BELOW, R.id.menu_tools);
                    optionItems.setPadding(dp(10), b ? 0 : dp(15), dp(10), b ? dp(15) : 0);
                } else {
                    params.addRule(RelativeLayout.BELOW, b ? R.id.menu_controls : R.id.menu_tools);
                }
                optionItems.setLayoutParams(params);
            }
            {
                LinearLayout optionControls = (LinearLayout)optionsMenu.findViewById(R.id.menu_controls);
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, dp(52));
                if(isTablet()) {
                    optionControls.setVisibility(View.GONE);
                } else {
                    optionControls.setVisibility(View.VISIBLE);
                    if(!b)
                        params.addRule(RelativeLayout.BELOW, R.id.menu_items);
                }
                optionControls.setLayoutParams(params);
            }
            {
                LinearLayout optionTools = (LinearLayout)optionsMenu.findViewById(R.id.menu_tools);
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, dp(52));
                if(isTablet()) {
                    optionTools.findViewById(R.id.menu_tools_menu).setVisibility(View.VISIBLE);
                    if(!b)
                        params.addRule(RelativeLayout.BELOW, R.id.menu_items);
                }
                else {
                    optionTools.findViewById(R.id.menu_tools_menu).setVisibility(View.GONE);
                    params.addRule(RelativeLayout.CENTER_HORIZONTAL);
                    if(b)
                        params.addRule(RelativeLayout.BELOW, R.id.menu_items);
                }
                optionTools.setLayoutParams(params);
            }
        }

        {
            if (isTablet()){
                findViewById(R.id.toolbar_back).setVisibility(View.VISIBLE);
                findViewById(R.id.toolbar_forward).setVisibility(View.VISIBLE);
                findViewById(R.id.toolbar_refresh).setVisibility(View.VISIBLE);
                findViewById(R.id.toolbar_stop).setVisibility(View.GONE);
                optionsMenu.findViewById(R.id.menu_control_back).setVisibility(View.GONE);
                optionsMenu.findViewById(R.id.menu_control_forward).setVisibility(View.GONE);
                optionsMenu.findViewById(R.id.menu_control_refresh).setVisibility(View.GONE);
                optionsMenu.findViewById(R.id.menu_control_stop).setVisibility(View.GONE);
            }else{
                findViewById(R.id.toolbar_back).setVisibility(View.GONE);
                findViewById(R.id.toolbar_forward).setVisibility(View.GONE);
                findViewById(R.id.toolbar_refresh).setVisibility(View.GONE);
                findViewById(R.id.toolbar_stop).setVisibility(View.GONE);
                optionsMenu.findViewById(R.id.menu_control_back).setVisibility(View.VISIBLE);
                optionsMenu.findViewById(R.id.menu_control_forward).setVisibility(View.VISIBLE);
                optionsMenu.findViewById(R.id.menu_control_refresh).setVisibility(View.VISIBLE);
                optionsMenu.findViewById(R.id.menu_control_stop).setVisibility(View.GONE);
            }
        }

        setFlag("urlbar_top", b);
    }

    public void toggleToolbarAutohide(View view) {
        Switch s = (Switch)view;
        setToolbarAutohide(s.isChecked());
    }

    public void setToolbarAutohide(boolean b) {
        Switch s = (Switch)settings.findViewById(R.id.settings_autohide);
        s.setChecked(b);
        setFlag("toolbar_autohide", b);
        showInterface();
    }

    public void setHomepage(String url) {
        EditText et = (EditText)settings.findViewById(R.id.settings_homepage);
        et.setText(url);
        setParam("homepage", url);
    }

    public void toggleDesktop(View view) {
        Switch s = (Switch)view;
        setDesktop(s.isChecked());
    }

    public void setDesktop(boolean b) {
        Switch s = (Switch)settings.findViewById(R.id.settings_desktop);
        s.setChecked(b);

        webView.getSettings().setUserAgentString(b ? "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) Gecko/20100101 Firefox/4.0" : null);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);

        setFlag("desktop_mode", b);
        this.reloadPage(null);
    }

    public void changeSearchProvider(View view) {
        if(view.getId() == R.id.search_duckduckgo)
            setSearchProvider("DuckDuckGo");
        else if(view.getId() == R.id.search_google)
            setSearchProvider("Google");
        else if(view.getId() == R.id.search_bing)
            setSearchProvider("Bing");
        else if(view.getId() == R.id.search_yahoo)
            setSearchProvider("Yahoo");
        else if(view.getId() == R.id.search_baidu)
            setSearchProvider("Baidu");

        searchSelect.dismiss();
    }

    public void setSearchProvider(String s) {
        ((TextView)settings.findViewById(R.id.settings_search)).setText(s);

        setParam("search_provider", s);
    }

    private void initiate() {
        if(!prefs.contains("javascript_enabled"))
            setFlag("javascript_enabled", true);

        if(!prefs.contains("private_mode"))
            setFlag("private_mode", false);

        if(!prefs.contains("desktop_mode"))
            setFlag("desktop_mode", false);

        if(!prefs.contains("urlbar_top"))
            setFlag("urlbar_top", true);

        if(!prefs.contains("toolbar_autohide"))
            setFlag("toolbar_autohide", true);

        if(!prefs.contains("homepage"))
            setParam("homepage", "http://start.duckduckgo.com/");

        if(!prefs.contains("search_provider"))
            setParam("search_provider", "DuckDuckGo");


        this.setJavascript(getFlag("javascript_enabled"));
        this.setPrivate(getFlag("private_mode"));
        this.setDesktop(getFlag("desktop_mode"));
        this.setUrlBar(getFlag("urlbar_top"));
        this.setToolbarAutohide(getFlag("toolbar_autohide"));
        this.setHomepage(getParam("homepage"));
        this.setSearchProvider(getParam("search_provider"));
    }

    private void setFlag(String k, boolean v) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(k, v);
        editor.apply();
    }

    protected boolean getFlag(String k) {
        return prefs.getBoolean(k, true);
    }

    private void setParam(String k, String v) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(k, v);
        editor.apply();
    }

    private String getParam(String k) {
        return prefs.getString(k, null);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        webView.saveState(state);
    }

    @Override
    public void onBackPressed() {
        if(searchSelect.isShowing())
            searchSelect.dismiss();
        else if(bookmarks.isShowing())
            bookmarks.dismiss();
        else if(history.isShowing())
            history.dismiss();
        else if(settings.isShowing())
            settings.dismiss();
        else if(help.isShowing())
            help.dismiss();
        else if(optionsMenu.isShowing())
            optionsMenu.dismiss();
        else if(webView.canGoBack())
            pageBack(null);
        else
            super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initiate();
    }

    public int dp(float dp) {
        return (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    public void doNothing(View view) {

    }

    public int colour(int colourResource) {
        return getResources().getColor(colourResource);
    }

    public boolean isTablet() {
        DisplayMetrics m = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(m);

        float x= m.widthPixels/m.xdpi;
        float y= m.heightPixels/m.ydpi;
        double d = Math.sqrt(x*x + y*y);

        return d>=6.5;
    }
}
