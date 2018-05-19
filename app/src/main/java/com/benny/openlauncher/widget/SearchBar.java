package com.benny.openlauncher.widget;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.benny.openlauncher.R;
import com.benny.openlauncher.interfaces.AppUpdateListener;
import com.benny.openlauncher.manager.Setup;
import com.benny.openlauncher.viewutil.IconLabelItem;
import com.benny.openlauncher.model.Item;
import com.benny.openlauncher.model.App;
import com.benny.openlauncher.util.AppSettings;
import com.benny.openlauncher.util.DragAction;
import com.benny.openlauncher.util.Tool;
import com.benny.openlauncher.viewutil.CircleDrawable;
import com.mikepenz.fastadapter.IItemAdapter;
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class SearchBar extends FrameLayout {

    private static final long ANIM_TIME = 200;
    public TextView _searchClock;
    public AppCompatImageView _switchButton;
    public AppCompatImageView _searchButton;
    public AppCompatEditText _searchInput;
    public RecyclerView _searchRecycler;
    private CircleDrawable _icon;
    private CardView _searchCardContainer;
    private FastItemAdapter<IconLabelItem> _adapter = new FastItemAdapter<>();
    private CallBack _callback;
    private boolean _expanded;
    private boolean _searchInternetEnabled = true;
    private Mode _mode = Mode.DateAll;
    private int _searchClockTextSize = 28;
    private float _searchClockSubTextFactor = 0.5f;

    public SearchBar(@NonNull Context context) {
        super(context);
        init();
    }

    public SearchBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SearchBar(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setCallback(CallBack callback) {
        _callback = callback;
    }

    public boolean collapse() {
        if (!_expanded) {
            return false;
        }
        _searchButton.callOnClick();
        return !_expanded;
    }

    private void init() {
        int dp1 = Tool.dp2px(1, getContext());
        int iconMarginOutside = dp1 * 16;
        int iconMarginTop = dp1 * 13;
        int searchTextHorizontalMargin = dp1 * 8;
        int searchTextMarginTop = dp1 * 4;
        int iconSize = dp1 * 24;
        int iconPadding = dp1 * 6; // CircleDrawable uses 6dp as well!!

        _searchClock = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.view_search_clock, this, false);
        _searchClock.setTextSize(TypedValue.COMPLEX_UNIT_DIP, _searchClockTextSize);
        LayoutParams clockParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clockParams.setMargins(iconMarginOutside, 0, 0, 0);
        clockParams.gravity = Gravity.START;

        LayoutParams switchButtonParams = null;
        // && Setup._appSettings().isSearchGridListSwitchEnabled()

        _switchButton = new AppCompatImageView(getContext());
        updateSwitchIcon();
        _switchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Setup.appSettings().setSearchUseGrid(!Setup.appSettings().isSearchUseGrid());
                updateSwitchIcon();
                updateRecyclerViewLayoutManager();
            }
        });
        _switchButton.setVisibility(View.GONE);
        _switchButton.setPadding(0, iconPadding, 0, iconPadding);

        switchButtonParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        switchButtonParams.setMargins(iconMarginOutside / 2, 0, 0, 0);
        switchButtonParams.gravity = Gravity.START | Gravity.CENTER_VERTICAL;

        if (isInEditMode()) return;
        _icon = new CircleDrawable(getContext(), getResources().getDrawable(R.drawable.ic_search_light_24dp), Color.BLACK);
        _searchButton = new AppCompatImageView(getContext());
        _searchButton.setImageDrawable(_icon);
        _searchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (_expanded && _searchInput.getText().length() > 0) {
                    _searchInput.getText().clear();
                    return;
                }
                _expanded = !_expanded;
                if (_expanded) {
                    expandInternal();
                } else {
                    collapseInternal();
                }
            }
        });
        LayoutParams buttonParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonParams.setMargins(0, iconMarginTop, iconMarginOutside, 0);
        buttonParams.gravity = Gravity.END;

        _searchCardContainer = new CardView(getContext());
        _searchCardContainer.setCardBackgroundColor(Color.TRANSPARENT);
        _searchCardContainer.setVisibility(View.GONE);
        _searchCardContainer.setRadius(0);
        _searchCardContainer.setCardElevation(0);
        _searchCardContainer.setContentPadding(dp1 * 4, dp1 * 4, dp1 * 4, dp1 * 4);

        _searchInput = new AppCompatEditText(getContext());
        _searchInput.setBackground(null);
        _searchInput.setHint(R.string.search_hint);
        _searchInput.setHintTextColor(Color.WHITE);
        _searchInput.setTextColor(Color.WHITE);
        _searchInput.setSingleLine();
        _searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                _adapter.filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        _searchInput.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event != null) && (event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    _callback.onInternetSearch(_searchInput.getText().toString());
                    _searchInput.getText().clear();
                    return true;
                }
                return false;
            }
        });
        LayoutParams inputCardParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        //(switchButton != null ? iconMarginOutside + iconSize : 0) + searchTextHorizontalMargin
        //iconMarginOutside + iconSize + searchTextHorizontalMargin
        inputCardParams.setMargins(0, searchTextMarginTop, 0, 0);

        LayoutParams inputParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inputParams.setMargins(iconMarginOutside + iconSize, 0, 0, 0);


        _searchCardContainer.addView(_switchButton, switchButtonParams);
        _searchCardContainer.addView(_searchInput, inputParams);

        initRecyclerView();

        Setup.appLoader().addUpdateListener(new AppUpdateListener() {
            @Override
            public boolean onAppUpdated(List<App> apps) {
                _adapter.clear();
                if (Setup.appSettings().getSearchBarShouldShowHiddenApps()) {
                    apps = Setup.appLoader().getAllApps(getContext(), true);
                }
                List<IconLabelItem> items = new ArrayList<>();
                if (_searchInternetEnabled) {
                    items.add(new IconLabelItem(getContext(), R.string.search_online)
                            .withIconGravity(Gravity.START)
                            .withOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    _callback.onInternetSearch(_searchInput.getText().toString());
                                    _searchInput.getText().clear();
                                }
                            })
                            .withTextColor(Color.WHITE)
                            .withIconPadding(getContext(), 8)
                            .withBold(true)
                            .withMatchParent(true)
                            .withTextGravity(Gravity.END));
                }
                for (int i = 0; i < apps.size(); i++) {
                    final App app = apps.get(i);
                    final int finalI = i;
                    items.add(new IconLabelItem(getContext(), app.getIcon(), app.getLabel(), 36)
                            .withIconGravity(Setup.appSettings().getSearchGridSize() > 1 && Setup.appSettings().getSearchLabelLines() == 0 ? Gravity.TOP : Gravity.START)
                            .withOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    startApp(v.getContext(), app);
                                }
                            })
                            .withOnLongClickListener(AppItemView.Builder.getLongClickDragAppListener(Item.newAppItem(app), DragAction.Action.APP, new AppItemView.LongPressCallBack() {
                                @Override
                                public boolean readyForDrag(View view) {
                                    if (finalI == -1) return false;

                                    _expanded = !_expanded;
                                    collapseInternal();
                                    return true;
                                }

                                @Override
                                public void afterDrag(View view) {
                                }
                            }))
                            .withTextColor(Color.WHITE)
                            .withMatchParent(true)
                            .withIconPadding(getContext(), 8)
                            .withMaxTextLines(Setup.appSettings().getSearchLabelLines()));
                }
                _adapter.set(items);

                return false;
            }
        });
        _adapter.getItemFilter().withFilterPredicate(new IItemAdapter.Predicate<IconLabelItem>() {
            @Override
            public boolean filter(IconLabelItem item, CharSequence constraint) {
                if (item._label.equals(getContext().getString(R.string.search_online)))
                    return false;
                String s = constraint.toString();
                if (s.isEmpty())
                    return true;
                else if (item._label.toLowerCase().contains(s.toLowerCase()))
                    return false;
                else
                    return true;
            }
        });

        final LayoutParams recyclerParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        addView(_searchClock, clockParams);
        addView(_searchRecycler, recyclerParams);
        addView(_searchCardContainer, inputCardParams);
        addView(_searchButton, buttonParams);

        _searchInput.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                _searchInput.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int marginTop = Tool.dp2px(56, getContext()) + _searchInput.getHeight();
                int marginBottom = Desktop._bottomInset;
                recyclerParams.setMargins(0, marginTop, 0, marginBottom);
                recyclerParams.height = ((View) getParent()).getHeight() - marginTop - marginBottom / 2;
                _searchRecycler.setLayoutParams(recyclerParams);
                _searchRecycler.setPadding(0, 0, 0, (int) (marginBottom * 1.5f));
            }
        });
    }

    private void collapseInternal() {
        if (_callback != null) {
            _callback.onCollapse();
        }
        _icon.setIcon(getResources().getDrawable(R.drawable.ic_search_light_24dp));
        Tool.visibleViews(ANIM_TIME, _searchClock);
        Tool.goneViews(ANIM_TIME, _searchCardContainer, _searchRecycler, _switchButton);
        _searchInput.getText().clear();
    }

    private void expandInternal() {
        if (_callback != null) {
            _callback.onExpand();
        }
        if (Setup.appSettings().isResetSearchBarOnOpen()) {
            RecyclerView.LayoutManager lm = _searchRecycler.getLayoutManager();
            if (lm instanceof LinearLayoutManager) {
                ((LinearLayoutManager) _searchRecycler.getLayoutManager()).scrollToPositionWithOffset(0, 0);
            } else if (lm instanceof GridLayoutManager) {
                ((GridLayoutManager) _searchRecycler.getLayoutManager()).scrollToPositionWithOffset(0, 0);
            }
        }
        _icon.setIcon(getResources().getDrawable(R.drawable.ic_clear_white_24dp));
        Tool.visibleViews(ANIM_TIME, _searchCardContainer, _searchRecycler, _switchButton);
        Tool.goneViews(ANIM_TIME, _searchClock);
    }

    private void updateSwitchIcon() {
        _switchButton.setImageResource(Setup.appSettings().isSearchUseGrid() ? R.drawable.ic_apps_white_48dp : R.drawable.ic_view_list_white_24dp);
    }

    private void updateRecyclerViewLayoutManager() {
        int gridSize = Setup.appSettings().isSearchUseGrid() ? Setup.appSettings().getSearchGridSize() : 1;
        if (gridSize == 1) {
            _searchRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        } else {
            _searchRecycler.setLayoutManager(new GridLayoutManager(getContext(), gridSize, GridLayoutManager.VERTICAL, false));
        }
        _searchRecycler.getLayoutManager().setAutoMeasureEnabled(false);
    }

    protected void initRecyclerView() {
        _searchRecycler = new RecyclerView(getContext());
        _searchRecycler.setItemAnimator(null);
        _searchRecycler.setVisibility(View.GONE);
        _searchRecycler.setAdapter(_adapter);
        _searchRecycler.setClipToPadding(false);
        _searchRecycler.setHasFixedSize(true);
        updateRecyclerViewLayoutManager();
    }

    protected void startApp(Context context, App app) {
        Tool.startApp(context, app);
    }

    public AppCompatImageView getSearchButton() {
        return _searchButton;
    }

    public void updateClock() {
        AppSettings appSettings = AppSettings.get();
        if (!appSettings.isSearchBarTimeEnabled()) {
            _searchClock.setText("");
            return;
        }

        if (_searchClock != null) {
            _searchClock.setTextColor(appSettings.getDesktopDateTextColor());
        }
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        SimpleDateFormat sdf = _mode.sdf;

        int mode = appSettings.getDesktopDateMode();
        if (mode >= 0 && mode < Mode.getCount()) {
            sdf = Mode.getById(mode).sdf;
            if (mode == 0) {
                sdf = appSettings.getUserDateFormat();
            }
        }

        if (sdf == null) {
            sdf = Setup.appSettings().getUserDateFormat();
        }
        String text = sdf.format(calendar.getTime());
        String[] lines = text.split("\n");
        Spannable span = new SpannableString(text);
        span.setSpan(new RelativeSizeSpan(_searchClockSubTextFactor), lines[0].length() + 1, lines[0].length() + 1 + lines[1].length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        _searchClock.setText(span);
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            int topInset = insets.getSystemWindowInsetTop();
            setPadding(getPaddingLeft(), topInset + Tool.dp2px(10, getContext()), getPaddingRight(), getPaddingBottom());
        }
        return insets;
    }

    public enum Mode {
        DateAll(1, new SimpleDateFormat("MMMM dd'\n'EEEE',' yyyy", Locale.getDefault())),
        DateNoYearAndTime(2, new SimpleDateFormat("MMMM dd'\n'HH':'mm", Locale.getDefault())),
        DateAllAndTime(3, new SimpleDateFormat("MMMM dd',' yyyy'\n'HH':'mm", Locale.getDefault())),
        TimeAndDateAll(4, new SimpleDateFormat("HH':'mm'\n'MMMM dd',' yyyy", Locale.getDefault())),
        Custom(0, null);

        SimpleDateFormat sdf;
        int id;

        Mode(int id, SimpleDateFormat sdf) {
            this.id = id;
            this.sdf = sdf;
        }

        public static Mode getById(int id) {
            for (int i = 0; i < values().length; i++) {
                if (values()[i].getId() == id)
                    return values()[i];
            }
            throw new RuntimeException("ID not found!");
        }


        public int getId() {
            return id;
        }

        public static int getCount() {
            return values().length;
        }
    }

    public interface CallBack {
        void onInternetSearch(String string);

        void onExpand();

        void onCollapse();
    }
}
