package com.abobrinha.caixinha.ui;

import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.navigation.NavigationView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

import com.abobrinha.caixinha.R;
import com.abobrinha.caixinha.data.PreferencesUtils;
import com.abobrinha.caixinha.network.SocialUtils;
import com.abobrinha.caixinha.sync.HistorySyncUtils;

public class MainActivity extends AppCompatActivity {

    private NavigationView mNavigationView;
    private boolean mIsHistoryPage = true;

    private final String IS_HISTORY_PAGE = "is_history_page";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeUIElements();

        if (savedInstanceState == null) {
            // possível intent recebido de um App Shortcut
            if (getIntent().hasExtra(getString(R.string.shortcut_intent))) {
                PreferencesUtils.setMainHistoryCategory(this,
                        getIntent()
                                .getIntExtra(getString(R.string.shortcut_intent),
                                        PreferencesUtils.CATEGORY_HISTORIES));
            }

            // possível intent recebido do FCM
            if (getIntent().hasExtra(getString(R.string.fcm_extra_key))) {
                SocialUtils.openExternalLink(this, SocialUtils.WEB, getIntent()
                        .getStringExtra(getString(R.string.fcm_extra_key)));
            }

            int category = PreferencesUtils.getMainHistoryCategory(this);
            int itemId = mNavigationView.getMenu().getItem(0).getSubMenu().getItem(category).getItemId();
            mNavigationView.setCheckedItem(itemId);
            loadHistories();
        } else {
            mIsHistoryPage = savedInstanceState.getBoolean(IS_HISTORY_PAGE, true);
        }

        HistorySyncUtils.initialize(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(IS_HISTORY_PAGE, mIsHistoryPage);
        super.onSaveInstanceState(outState);
    }

    private void initializeUIElements() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final CollapsingToolbarLayout collapsingToolbarLayout =
                (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.app_bar_layout);
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            boolean isShow = true;
            int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }
                if (scrollRange + verticalOffset == 0) {
                    collapsingToolbarLayout.setTitle(getString(R.string.app_name));
                    isShow = true;
                } else if (isShow) {
                    collapsingToolbarLayout.setTitle(" ");
                    isShow = false;
                }
            }
        });

        mNavigationView = (NavigationView) findViewById(R.id.navigation_view);
        final DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer);

        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                drawerLayout.closeDrawers();

                switch (menuItem.getItemId()) {
                    case R.id.menu_all_histories:
                        if (!menuItem.isChecked()) {
                            menuItem.setChecked(true);
                            PreferencesUtils.setMainHistoryCategory(MainActivity.this,
                                    PreferencesUtils.CATEGORY_HISTORIES);
                            loadHistories();
                        }
                        break;

                    case R.id.menu_favorites:
                        if (!menuItem.isChecked()) {
                            menuItem.setChecked(true);
                            PreferencesUtils.setMainHistoryCategory(MainActivity.this,
                                    PreferencesUtils.CATEGORY_FAVORITES);
                            loadHistories();
                        }
                        break;

                    case R.id.menu_settings:
                        startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                        break;

                    case R.id.menu_share:
                        SocialUtils.shareApp(MainActivity.this);
                        break;

                    case R.id.menu_about:
                        loadContacts();
                        break;
                }
                return true;
            }
        });

        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this,
                drawerLayout, toolbar, R.string.open_main_drawer, R.string.close_main_drawer) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
        };

        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
    }

    private void loadHistories() {
        HistoryGridFragment historyGridFragment = new HistoryGridFragment();

        FragmentManager fragmentManager = getSupportFragmentManager();

        fragmentManager.beginTransaction()
                .replace(R.id.frame, historyGridFragment)
                .commit();
    }

    private void loadContacts() {
        mIsHistoryPage = false;
        SubMenu drawerSubMenu = mNavigationView.getMenu().getItem(0).getSubMenu();
        for (int i = 0; i < drawerSubMenu.size(); i++) {
            drawerSubMenu.getItem(i).setChecked(false);
        }

        Fragment contactsFragment = new ContactsFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.frame, contactsFragment)
                .commit();
    }

    /*
     * Controla o comportamento do botão Back. Como o drawer permite exibir outras opções além de
     * histórias, é esperado que o aplicativo se encerre com o Back apenas quando estiver exibindo
     * uma categoria de histórias, caso contrário, retorna para a última categoria aberta pelo
     * usuário.
     */
    @Override
    public void onBackPressed() {
        if (mIsHistoryPage) {
            super.onBackPressed();
            return;
        }

        mIsHistoryPage = true;
        int category = PreferencesUtils.getMainHistoryCategory(this);
        SubMenu drawerSubMenu = mNavigationView.getMenu().getItem(0).getSubMenu();
        int itemId = drawerSubMenu.getItem(category).getItemId();
        drawerSubMenu.getItem(category).setChecked(true);
        mNavigationView.setCheckedItem(itemId);
        loadHistories();
    }
}