package com.ideasfrombrain.search_based_launcher_v3;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("Convert2Lambda")
public class AppListManager {
    final MainActivity mainActivity;
    private final AppListView appListView;
    private final DialogFactory dialogFactory;
    List<App> pkg = getEmptyAppList();
    List<App> filtered = getEmptyAppList();
    Set<App> extra = new HashSet<>();
    Set<App> hidden = new HashSet<>();
    final List<App> recent = getEmptyAppList();
    final private PreferencesAdapter preferencesAdapter;
    public static final App MENU_APP = new App(MainActivity.APP_PACKAGE_NAME + ".Menu", " Menu-Launcher", MainActivity.APP_PACKAGE_NAME + ".Menu");

    public AppListManager(MainActivity mainActivity, Bundle savedInstanceState) {
        this.mainActivity = mainActivity;
        preferencesAdapter = new PreferencesAdapter(mainActivity);
        appListView = new AppListView(mainActivity);
        dialogFactory = new DialogFactory(mainActivity);
        loadFromSavedState(savedInstanceState);
    }

    public void generateFiltered() {
        filtered.clear();
        String filterText = mainActivity.getSearchText().getFilterText();
        for (App app: recent) {
            if (app.getNick().toLowerCase().matches(filterText)) {
                filtered.add(app.getAsRecent());
            }
        }
        for (App app: pkg) {
            if (app.getNick().toLowerCase().matches(filterText) && !recent.contains(app)) {
                filtered.add(app);
            }
        }
        if (filtered.size() == 1 && mainActivity.getAutostartButton().isOn()) {
            runApp(0);
        } else {
            appListView.viewAppList(filtered);
        }
    }

    public void refreshView() {
        final Intent main = new Intent(Intent.ACTION_MAIN, null);
        final PackageManager pm = mainActivity.getPackageManager();
        switch (mainActivity.getMenu().getAppListSelector().getSelected()) {
            case 0:
                pkg = getApplicationActivities(main, pm);
                pkg.removeAll(hidden);
                pkg.addAll(extra);
                break;
            case 1:
                pkg = getAllActivities(main, pm);
                pkg.removeAll(hidden);
                pkg.addAll(extra);
                break;
            case 2:
                pkg.addAll(extra);
                break;
            case 3:
                pkg.addAll(hidden);
                break;
        }
        recent.retainAll(pkg);
        pkg.add(MENU_APP);
        generateFiltered();
    }

    private List<App> getAllActivities(Intent main, PackageManager pm) {
        final List<App> pkg = getEmptyAppList();
        final List<ResolveInfo> launchables = pm.queryIntentActivities(main, 0);
        for (ResolveInfo launchable : launchables) {
            App app = new App(launchable.activityInfo.packageName, deriveNick(launchable), launchable.activityInfo.name);
            pkg.add(app);
        }
        return pkg;
    }

    private static List<App> getEmptyAppList() {
        return new ArrayList<>();
    }

    private String deriveNick(ResolveInfo launchable) {
        String[] split = launchable.activityInfo.name.split("\\.");
        String nick = split[1];
        for (int j = 2; j < split.length; j++) {
            nick = nick + ":" + split[j];
        }
        return nick;
    }

    private List<App> getApplicationActivities(Intent main, PackageManager pm) {
        final List<App> pkg = getEmptyAppList();
        main.addCategory(Intent.CATEGORY_LAUNCHER); // will show only Regular AppListManager
        final List<ResolveInfo> launchables = pm.queryIntentActivities(main, 0);

        for (ResolveInfo launchable : launchables) {
            String nick = launchable.activityInfo.loadLabel(pm).toString();
            String name = launchable.activityInfo.packageName;
            String activity = launchable.activityInfo.name;
            final App app = new App(name, nick, activity);
            pkg.add(app);
        }
        return pkg;
    }

    public void load() {
        try {
            extra = preferencesAdapter.loadSet("extra");
            hidden = preferencesAdapter.loadSet("hidden");
        } catch (Exception e) {
            save();
        }
        refreshView();
    }

    public void save() {
        preferencesAdapter.saveSet(extra, "extra");
        preferencesAdapter.saveSet(hidden, "hidden");
    }

    private void loadFromSavedState(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            load();
        } else {
            pkg = new ArrayList<>(App.getApps(new HashSet<>(savedInstanceState.getStringArrayList("pkg"))));
            extra = App.getApps(new HashSet<>(savedInstanceState.getStringArrayList("extra")));
            hidden = App.getApps(new HashSet<>(savedInstanceState.getStringArrayList("hidden")));
        }
    }

    public void runApp(int appIndex) {
        final App app = filtered.get(appIndex);
        mainActivity.getSearchText().setActivatedColor();
        recent.remove(app);
        if (app.isMenu()) {
            mainActivity.getMenu().toggle(false);
        } else {
            try {
                final Intent intent = new Intent(Intent.ACTION_MAIN, null);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setComponent(new ComponentName(app.getName(), app.getActivity()));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                mainActivity.startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
                mainActivity.getSearchText().setNormalColor();
            }
        }
    }
    
    public void saveState(Bundle savedInstanceState) {
        savedInstanceState.putStringArrayList("pkg", new ArrayList<>(App.getJson(new HashSet<>(pkg))));
        savedInstanceState.putStringArrayList("extra", new ArrayList<>(App.getJson(extra)));
        savedInstanceState.putStringArrayList("hidden", new ArrayList<>(App.getJson(hidden)));
    }

    public boolean showOptionsForApp(final int appIndex) {
        final App app = filtered.get(appIndex);
        if (app.equals(MENU_APP)) {
            return false;
        }

        switch (mainActivity.getMenu().getAppListSelector().getSelected()) {
            case 0:
                dialogFactory.showNormalOptions(app);
                break;
            case 1:
                dialogFactory.showAddExtraAppOptions(app);
                break;
            case 2:
                dialogFactory.showRemoveExtraAppOptions(app);
                break;
            case 3:
                dialogFactory.showUnhideAppOptions(app);
                break;
        }
        return false;
    }

    public Set<App> getExtra() {
        return extra;
    }

    public Set<App> getHidden() {
        return hidden;
    }

    public List<App> getRecent() {
        return recent;
    }
}