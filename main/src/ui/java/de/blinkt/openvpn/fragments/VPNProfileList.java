package de.blinkt.openvpn.fragments;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.view.animation.LinearInterpolator;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

import de.blinkt.openvpn.LaunchVPN;
import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.activities.ConfigConverter;
import de.blinkt.openvpn.activities.DisconnectVPN;
import de.blinkt.openvpn.activities.FileSelect;
import de.blinkt.openvpn.activities.VPNPreferences;
import de.blinkt.openvpn.activities.mine.VPNProfileAdapter;
import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.DisconnectVPNService;
import de.blinkt.openvpn.core.PasswordDialogFragment;
import de.blinkt.openvpn.core.Preferences;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VpnStatus;

import static de.blinkt.openvpn.core.ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT;
import static de.blinkt.openvpn.core.OpenVPNService.DISCONNECT_VPN;
import static de.blinkt.openvpn.core.OpenVPNService.EXTRA_CHALLENGE_TXT;
import static de.blinkt.openvpn.core.OpenVPNService.EXTRA_START_REASON;

public class VPNProfileList extends Fragment implements OnClickListener, VpnStatus.StateListener, VPNProfileAdapter.OnItemClickListener {

    public final static int RESULT_VPN_DELETED = Activity.RESULT_FIRST_USER;
    public final static int RESULT_VPN_DUPLICATE = Activity.RESULT_FIRST_USER + 1;
    final static int SHORTCUT_VERSION = 1;
    private static final int MENU_ADD_PROFILE = Menu.FIRST;
    private static final int START_VPN_CONFIG = 92;
    private static final int SELECT_PROFILE = 43;
    private static final int IMPORT_PROFILE = 231;
    private static final int FILE_PICKER_RESULT_KITKAT = 392;
    private static final int MENU_IMPORT_PROFILE = Menu.FIRST + 1;
    private static final int MENU_CHANGE_SORTING = Menu.FIRST + 2;
    private static final int MENU_IMPORT_AS = Menu.FIRST + 3;
    private static final String PREF_SORT_BY_LRU = "sortProfilesByLRU";
    protected VpnProfile mEditProfile = null;
    private String mLastStatusMessage;
    private VPNProfileAdapter adapter;
    private Intent mLastIntent;
    private VpnProfile defaultVPN;
    private View mPermissionView;
    private ActivityResultLauncher<String> mPermReceiver;
    private RecyclerView recyclerView;
    private List<VpnProfile> vpnProfiles;
    private VpnProfile lastUsedProfile;
    private FrameLayout lastUsedProfileContainer;
    private View lastUsedProfileView;
    private TextView cardViewItemSubtitle;
    private String lastUsedProfileState;
    private int lastUsedProfileStateColor;
    private MaterialButton connectButton;
    private boolean isConnecting = false;

    @Override
    public void updateState(String state, String logmessage, final int localizedResId, ConnectionStatus level, Intent intent) {
        requireActivity().runOnUiThread(() -> {
            mLastStatusMessage = VpnStatus.getLastCleanLogMessage(getActivity());
            mLastIntent = intent;
            adapter.setLastStatusMessage(mLastStatusMessage); // Передаем статус адаптеру
            adapter.notifyDataSetChanged();
            showUserRequestDialogIfNeeded(level, intent);
            loadLastUsedProfile();
            populateLastUsedProfile(lastUsedProfile);
            cardViewItemSubtitle = lastUsedProfileView.findViewById(R.id.vpn_item_subtitle);

            if (level == ConnectionStatus.LEVEL_CONNECTED) {
                connectButton.setText(R.string.disconnect_btn_text);
                connectButton.setBackgroundColor(getResources().getColor(R.color.btn_accent));
                lastUsedProfileState = getString(R.string.last_conf_state_connected);
                lastUsedProfileStateColor = getResources().getColor(R.color.state_connected);
                isConnecting = false;
            } else if (level == ConnectionStatus.LEVEL_NOTCONNECTED) {
                connectButton.setText(R.string.connect_btn_text);
                connectButton.setBackgroundColor(getResources().getColor(R.color.btn_accent));
                lastUsedProfileState = getString(R.string.last_conf_state_disconnected);
                lastUsedProfileStateColor = getResources().getColor(R.color.state_disconnected);
                isConnecting = false;
            } else {
                connectButton.setText(R.string.cancel_conn_btn_text);
                lastUsedProfileState = getString(R.string.last_conf_state_connecting);
                lastUsedProfileStateColor = getResources().getColor(R.color.state_connecting);
                connectButton.setBackgroundColor(getResources().getColor(R.color.btn_gray));
            }
            cardViewItemSubtitle.setTextColor(lastUsedProfileStateColor);
            cardViewItemSubtitle.setText(lastUsedProfileState);
            System.out.println(lastUsedProfileStateColor);
        });
    }

    private boolean showUserRequestDialogIfNeeded(ConnectionStatus level, Intent intent) {
        if (level == LEVEL_WAITING_FOR_USER_INPUT) {
            if (intent != null && intent.getStringExtra(EXTRA_CHALLENGE_TXT) != null) {
                PasswordDialogFragment pwInputFrag = PasswordDialogFragment.Companion.newInstance(intent, false);
                pwInputFrag.show(getParentFragmentManager(), "dialog");
                return true;
            }
        }
        return false;
    }

    @Override
    public void setConnectedVPN(String uuid) {
    }

    private void startOrStopVPN(VpnProfile profile) {
        saveLastUsedProfile(profile.getUUIDString());
        if (VpnStatus.isVPNActive() && profile.getUUIDString().equals(VpnStatus.getLastConnectedVPNProfile())) {
            if (mLastIntent != null) {
                startActivity(mLastIntent);
            } else {
                Intent disconnectIntent = new Intent(getActivity(), DisconnectVPNService.class);
                Objects.requireNonNull(getActivity()).startService(disconnectIntent);
            }
        } else {
            startVPN(profile);
        }
    }


    private void startButtonAnimation() {
        ObjectAnimator animator = ObjectAnimator.ofArgb(connectButton, "backgroundColor", Color.GRAY, Color.BLUE);
        animator.setDuration(1000);
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.start();
        isConnecting = true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        registerPermissionReceiver();
    }

    private void registerPermissionReceiver() {
        mPermReceiver = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                result -> checkForNotificationPermission(requireView()));
    }

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    void updateDynamicShortcuts() {
        PersistableBundle versionExtras = new PersistableBundle();
        versionExtras.putInt("version", SHORTCUT_VERSION);

        ShortcutManager shortcutManager = getContext().getSystemService(ShortcutManager.class);
        if (shortcutManager.isRateLimitingActive())
            return;

        List<ShortcutInfo> shortcuts = shortcutManager.getDynamicShortcuts();
        int maxvpn = shortcutManager.getMaxShortcutCountPerActivity() - 1;

        ShortcutInfo disconnectShortcut = new ShortcutInfo.Builder(getContext(), "disconnectVPN")
                .setShortLabel("Disconnect")
                .setLongLabel("Disconnect VPN")
                .setIntent(new Intent(getContext(), DisconnectVPN.class).setAction(DISCONNECT_VPN))
                .setIcon(Icon.createWithResource(getContext(), R.drawable.ic_shortcut_cancel))
                .setExtras(versionExtras)
                .build();

        LinkedList<ShortcutInfo> newShortcuts = new LinkedList<>();
        LinkedList<ShortcutInfo> updateShortcuts = new LinkedList<>();

        LinkedList<String> removeShortcuts = new LinkedList<>();
        LinkedList<String> disableShortcuts = new LinkedList<>();

        boolean addDisconnect = true;

        TreeSet<VpnProfile> sortedProfilesLRU = new TreeSet<VpnProfile>(new VpnProfileLRUComparator());
        ProfileManager profileManager = ProfileManager.getInstance(getContext());
        sortedProfilesLRU.addAll(profileManager.getProfiles());

        LinkedList<VpnProfile> LRUProfiles = new LinkedList<>();
        maxvpn = Math.min(maxvpn, sortedProfilesLRU.size());

        for (int i = 0; i < maxvpn; i++) {
            LRUProfiles.add(sortedProfilesLRU.pollFirst());
        }

        for (ShortcutInfo shortcut : shortcuts) {
            if (shortcut.getId().equals("disconnectVPN")) {
                addDisconnect = false;
                if (shortcut.getExtras() == null
                        || shortcut.getExtras().getInt("version") != SHORTCUT_VERSION)
                    updateShortcuts.add(disconnectShortcut);

            } else {
                VpnProfile p = ProfileManager.get(getContext(), shortcut.getId());
                if (p == null || p.profileDeleted) {
                    if (shortcut.isEnabled()) {
                        disableShortcuts.add(shortcut.getId());
                        removeShortcuts.add(shortcut.getId());
                    }
                    if (!shortcut.isPinned())
                        removeShortcuts.add(shortcut.getId());
                } else {

                    if (LRUProfiles.contains(p))
                        LRUProfiles.remove(p);
                    else
                        removeShortcuts.add(p.getUUIDString());

                    if (!p.getName().equals(shortcut.getShortLabel())
                            || shortcut.getExtras() == null
                            || shortcut.getExtras().getInt("version") != SHORTCUT_VERSION)
                        updateShortcuts.add(createShortcut(p));

                }
            }
        }
        if (addDisconnect)
            newShortcuts.add(disconnectShortcut);
        for (VpnProfile p : LRUProfiles)
            newShortcuts.add(createShortcut(p));

        if (updateShortcuts.size() > 0)
            shortcutManager.updateShortcuts(updateShortcuts);
        if (removeShortcuts.size() > 0)
            shortcutManager.removeDynamicShortcuts(removeShortcuts);
        if (newShortcuts.size() > 0)
            shortcutManager.addDynamicShortcuts(newShortcuts);
        if (disableShortcuts.size() > 0)
            shortcutManager.disableShortcuts(disableShortcuts, "VpnProfile does not exist anymore.");
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    ShortcutInfo createShortcut(VpnProfile profile) {
        Intent shortcutIntent = new Intent(Intent.ACTION_MAIN);
        shortcutIntent.setClass(requireContext(), LaunchVPN.class);
        shortcutIntent.putExtra(LaunchVPN.EXTRA_KEY, profile.getUUID().toString());
        shortcutIntent.setAction(Intent.ACTION_MAIN);
        shortcutIntent.putExtra(EXTRA_START_REASON, "shortcut");
        shortcutIntent.putExtra("EXTRA_HIDELOG", true);

        PersistableBundle versionExtras = new PersistableBundle();
        versionExtras.putInt("version", SHORTCUT_VERSION);

        return new ShortcutInfo.Builder(getContext(), profile.getUUIDString())
                .setShortLabel(profile.getName())
                .setLongLabel(getString(R.string.qs_connect, profile.getName()))
                .setIcon(Icon.createWithResource(getContext(), R.drawable.ic_shortcut_vpn_key))
                .setIntent(shortcutIntent)
                .setExtras(versionExtras)
                .build();
    }

    @Override
    public void onResume() {
        super.onResume();
        populateVpnList();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            updateDynamicShortcuts();
        }
        VpnStatus.addStateListener(this);
        defaultVPN = ProfileManager.getAlwaysOnVPN(requireContext());
        adapter.setDefaultVPN(defaultVPN); // Устанавливаем defaultVPN в адаптере

        // Загрузка последней использованной конфигурации
        loadLastUsedProfile();
        if (lastUsedProfile != null) {
            populateLastUsedProfile(lastUsedProfile);
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        VpnStatus.removeStateListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.vpn_profile_list, container, false);
        lastUsedProfileView = inflater.inflate(R.layout.vpn_list_item, lastUsedProfileContainer, false);


        TextView newvpntext = (TextView) v.findViewById(R.id.add_new_vpn_hint);
        TextView importvpntext = (TextView) v.findViewById(R.id.import_vpn_hint);

        newvpntext.setText(Html.fromHtml(getString(R.string.add_new_vpn_hint), new MiniImageGetter(), null));
        importvpntext.setText(Html.fromHtml(getString(R.string.vpn_import_hint), new MiniImageGetter(), null));

        ImageButton fab_add = (ImageButton) v.findViewById(R.id.fab_add);
        ImageButton fab_import = (ImageButton) v.findViewById(R.id.fab_import);
        if (fab_add != null)
            fab_add.setOnClickListener(this);

        if (fab_import != null)
            fab_import.setOnClickListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            checkForNotificationPermission(v);

        recyclerView = v.findViewById(R.id.vpn_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        vpnProfiles = new LinkedList<>();
        adapter = new VPNProfileAdapter(getContext(), vpnProfiles, this);
        recyclerView.setAdapter(adapter);

        // Инициализация контейнера для последней использованной конфигурации
        lastUsedProfileContainer = v.findViewById(R.id.last_used_profile_container);
        connectButton = v.findViewById(R.id.connect_btn);
        connectButton.setOnClickListener(view -> {
            if (lastUsedProfile != null) {
                if (isConnecting) {
                    // Отмена подключения
                    VpnStatus.updateStateString("USER_VPN_CANCEL", "", 0, ConnectionStatus.LEVEL_NOTCONNECTED);
                } else {
                    startOrStopVPN(lastUsedProfile);
                }
            }
        });

        return v;
    }

    private void checkForNotificationPermission(View v) {
        mPermissionView = v.findViewById(R.id.notification_permission);
        boolean permissionGranted =  (requireActivity().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED);
        mPermissionView.setVisibility(permissionGranted ? View.GONE : View.VISIBLE);

        mPermissionView.setOnClickListener((view) -> {
            mPermReceiver.launch(Manifest.permission.POST_NOTIFICATIONS);
        });
    }

    private void populateVpnList() {
        boolean sortByLRU = Preferences.getDefaultSharedPreferences(requireActivity()).getBoolean(PREF_SORT_BY_LRU, false);
        getPM().refreshVPNList(requireContext());
        Collection<VpnProfile> allvpn = getPM().getProfiles();
        TreeSet<VpnProfile> sortedset;
        if (sortByLRU)
            sortedset = new TreeSet<>(new VpnProfileLRUComparator());
        else
            sortedset = new TreeSet<>(new VpnProfileNameComparator());

        sortedset.addAll(allvpn);
        vpnProfiles.clear();
        vpnProfiles.addAll(sortedset);

        adapter.notifyDataSetChanged();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, @NonNull MenuInflater inflater) {
        menu.add(0, MENU_ADD_PROFILE, 0, R.string.menu_add_profile)
                .setIcon(R.drawable.ic_menu_add)
                .setAlphabeticShortcut('a')
                .setTitleCondensed(getActivity().getString(R.string.add))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        menu.add(0, MENU_IMPORT_PROFILE, 0, R.string.menu_import)
                .setIcon(R.drawable.ic_menu_import)
                .setAlphabeticShortcut('i')
                .setTitleCondensed(getActivity().getString(R.string.menu_import_short))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        menu.add(0, MENU_CHANGE_SORTING, 0, R.string.change_sorting)
                .setIcon(R.drawable.ic_sort)
                .setAlphabeticShortcut('s')
                .setTitleCondensed(getString(R.string.sort))
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        menu.add(0, MENU_IMPORT_AS, 0, R.string.import_from_as)
                .setIcon(R.drawable.ic_menu_import)
                .setAlphabeticShortcut('p')
                .setTitleCondensed("Import AS")
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == MENU_ADD_PROFILE) {
            onAddOrDuplicateProfile(null);
            return true;
        } else if (itemId == MENU_IMPORT_PROFILE) {
            return startImportConfigFilePicker();
        } else if (itemId == MENU_CHANGE_SORTING) {
            return changeSorting();
        } else if (itemId == MENU_IMPORT_AS) {
            return startASProfileImport();
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private boolean startASProfileImport() {
        ImportRemoteConfig asImportFrag = ImportRemoteConfig.newInstance(null);
        asImportFrag.show(getParentFragmentManager(), "dialog");
        return true;
    }

    private boolean changeSorting() {
        SharedPreferences prefs = Preferences.getDefaultSharedPreferences(requireActivity());
        boolean oldValue = prefs.getBoolean(PREF_SORT_BY_LRU, false);
        SharedPreferences.Editor prefsedit = prefs.edit();
        if (oldValue) {
            Toast.makeText(getActivity(), R.string.sorted_az, Toast.LENGTH_SHORT).show();
            prefsedit.putBoolean(PREF_SORT_BY_LRU, false);
        } else {
            prefsedit.putBoolean(PREF_SORT_BY_LRU, true);
            Toast.makeText(getActivity(), R.string.sorted_lru, Toast.LENGTH_SHORT).show();
        }
        prefsedit.apply();
        populateVpnList();
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab_import:
                startImportConfigFilePicker();
                break;
            case R.id.fab_add:
                onAddOrDuplicateProfile(null);
                break;
        }
    }

    private boolean startImportConfigFilePicker() {
        boolean startOldFileDialog = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && !Utils.alwaysUseOldFileChooser(getActivity()))
            startOldFileDialog = !startFilePicker();

        if (startOldFileDialog)
            startImportConfig();

        return true;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private boolean startFilePicker() {
        Intent i = Utils.getFilePickerIntent(getActivity(), Utils.FileType.OVPN_CONFIG);
        if (i != null) {
            startActivityForResult(i, FILE_PICKER_RESULT_KITKAT);
            return true;
        } else
            return false;
    }

    private void startImportConfig() {
        Intent intent = new Intent(getActivity(), FileSelect.class);
        intent.putExtra(FileSelect.NO_INLINE_SELECTION, true);
        intent.putExtra(FileSelect.WINDOW_TITLE, R.string.import_configuration_file);
        startActivityForResult(intent, SELECT_PROFILE);
    }

    private void onAddOrDuplicateProfile(final VpnProfile mCopyProfile) {
        Context context = getActivity();
        if (context != null) {
            final EditText entry = new EditText(context);
            entry.setSingleLine();

            AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            if (mCopyProfile == null)
                dialog.setTitle(R.string.menu_add_profile);
            else {
                dialog.setTitle(context.getString(R.string.duplicate_profile_title, mCopyProfile.mName));
                entry.setText(getString(R.string.copy_of_profile, mCopyProfile.mName));
            }

            dialog.setMessage(R.string.add_profile_name_prompt);
            dialog.setView(entry);

            dialog.setNeutralButton(R.string.menu_import_short,
                    (dialog1, which) -> startImportConfigFilePicker());
            dialog.setPositiveButton(android.R.string.ok,
                    (dialog12, which) -> {
                        String name = entry.getText().toString();
                        if (getPM().getProfileByName(name) == null) {
                            VpnProfile profile;
                            if (mCopyProfile != null) {
                                profile = mCopyProfile.copy(name);
                                profile.mProfileCreator = null;
                                profile.mUserEditable = true;
                            } else
                                profile = new VpnProfile(name);

                            addProfile(profile);
                            editVPN(profile);
                        } else {
                            Toast.makeText(getActivity(), R.string.duplicate_profile_name, Toast.LENGTH_LONG).show();
                        }
                    });
            dialog.setNegativeButton(android.R.string.cancel, null);
            dialog.create().show();
        }
    }

    private void addProfile(VpnProfile profile) {
        getPM().addProfile(profile);
        getPM().saveProfileList(getActivity());
        getPM().saveProfile(getActivity(), profile);
        vpnProfiles.add(profile);
        adapter.notifyDataSetChanged();
    }

    private ProfileManager getPM() {
        return ProfileManager.getInstance(getActivity());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_VPN_DELETED) {
            if (mEditProfile != null)
                vpnProfiles.remove(mEditProfile);
            adapter.notifyDataSetChanged();
        } else if (resultCode == RESULT_VPN_DUPLICATE && data != null) {
            String profileUUID = data.getStringExtra(VpnProfile.EXTRA_PROFILEUUID);
            VpnProfile profile = ProfileManager.get(getActivity(), profileUUID);
            if (profile != null)
                onAddOrDuplicateProfile(profile);
        }

        if (resultCode != Activity.RESULT_OK)
            return;

        if (requestCode == START_VPN_CONFIG) {
            String configuredVPN = data.getStringExtra(VpnProfile.EXTRA_PROFILEUUID);

            VpnProfile profile = ProfileManager.get(getActivity(), configuredVPN);
            getPM().saveProfile(getActivity(), profile);
            populateVpnList();

        } else if (requestCode == SELECT_PROFILE) {
            String fileData = data.getStringExtra(FileSelect.RESULT_DATA);
            Uri uri = new Uri.Builder().path(fileData).scheme("file").build();

            startConfigImport(uri);
        } else if (requestCode == IMPORT_PROFILE) {
            String profileUUID = data.getStringExtra(VpnProfile.EXTRA_PROFILEUUID);
            vpnProfiles.add(ProfileManager.get(getActivity(), profileUUID));
            adapter.notifyDataSetChanged();
        } else if (requestCode == FILE_PICKER_RESULT_KITKAT) {
            if (data != null) {
                Uri uri = data.getData();
                startConfigImport(uri);
            }
        }
    }

    private void startConfigImport(Uri uri) {
        Intent startImport = new Intent(getActivity(), ConfigConverter.class);
        startImport.setAction(ConfigConverter.IMPORT_PROFILE);
        startImport.setData(uri);
        startActivityForResult(startImport, IMPORT_PROFILE);
    }

    private void editVPN(VpnProfile profile) {
        mEditProfile = profile;
        Intent vprefintent = new Intent(getActivity(), VPNPreferences.class)
                .putExtra(getActivity().getPackageName() + ".profileUUID", profile.getUUID().toString());

        startActivityForResult(vprefintent, START_VPN_CONFIG);
    }

    private void startVPN(VpnProfile profile) {
        getPM().saveProfile(getActivity(), profile);

        Intent intent = new Intent(getActivity(), LaunchVPN.class);
        intent.putExtra(LaunchVPN.EXTRA_KEY, profile.getUUID().toString());
        intent.putExtra(EXTRA_START_REASON, "main profile list");
        intent.setAction(Intent.ACTION_MAIN);
        startActivity(intent);
    }

    private void loadLastUsedProfile() {
        SharedPreferences prefs = Preferences.getDefaultSharedPreferences(requireContext());
        String lastUsedProfileUUID = prefs.getString("last_used_vpn_profile", null);
        if (lastUsedProfileUUID != null) {
            lastUsedProfile = ProfileManager.get(getContext(), lastUsedProfileUUID);
            if (lastUsedProfile == null) {
                // Обработка ситуации, когда профиль не найден
                prefs.edit().remove("last_used_vpn_profile").apply();
            }
        }
    }


    private void saveLastUsedProfile(String uuid) {
        SharedPreferences prefs = Preferences.getDefaultSharedPreferences(requireContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("last_used_vpn_profile", uuid);
        editor.apply();
    }

    private void populateLastUsedProfile(VpnProfile profile) {
        if (profile == null) {
            // Обработка ситуации, когда профиль равен null
            return;
        }

        lastUsedProfileContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        lastUsedProfileView = inflater.inflate(R.layout.vpn_list_item, lastUsedProfileContainer, false);

        TextView title = lastUsedProfileView.findViewById(R.id.vpn_item_title);
        cardViewItemSubtitle = lastUsedProfileView.findViewById(R.id.vpn_item_subtitle);
        cardViewItemSubtitle.setText(lastUsedProfileState);
        cardViewItemSubtitle.setTextColor(lastUsedProfileStateColor);
        System.out.println(lastUsedProfileStateColor);

        ImageView quickEditSettings = lastUsedProfileView.findViewById(R.id.quickedit_settings);

        title.setText(profile.getName());
        quickEditSettings.setOnClickListener(v -> editVPN(profile));

        lastUsedProfileContainer.addView(lastUsedProfileView);
    }


    static class VpnProfileNameComparator implements Comparator<VpnProfile> {
        @Override
        public int compare(VpnProfile lhs, VpnProfile rhs) {
            if (lhs == rhs)
                return 0;
            if (lhs == null)
                return -1;
            if (rhs == null)
                return 1;
            if (lhs.mName == null)
                return -1;
            if (rhs.mName == null)
                return 1;
            return lhs.mName.compareTo(rhs.mName);
        }
    }

    static class VpnProfileLRUComparator implements Comparator<VpnProfile> {
        VpnProfileNameComparator nameComparator = new VpnProfileNameComparator();

        @Override
        public int compare(VpnProfile lhs, VpnProfile rhs) {
            if (lhs == rhs)
                return 0;
            if (lhs == null)
                return -1;
            if (rhs == null)
                return 1;
            if (lhs.mLastUsed > rhs.mLastUsed)
                return -1;
            if (lhs.mLastUsed < rhs.mLastUsed)
                return 1;
            else
                return nameComparator.compare(lhs, rhs);
        }
    }

    class MiniImageGetter implements Html.ImageGetter {
        @Override
        public Drawable getDrawable(String source) {
            Drawable d = null;
            if ("ic_menu_add".equals(source))
                d = requireActivity().getResources().getDrawable(R.drawable.ic_menu_add_grey, requireActivity().getTheme());
            else if ("ic_menu_archive".equals(source))
                d = requireActivity().getResources().getDrawable(R.drawable.ic_menu_import_grey, requireActivity().getTheme());
            if (d != null) {
                d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
                return d;
            } else {
                return null;
            }
        }
    }

    @Override
    public void onItemClick(VpnProfile profile) {
        startOrStopVPN(profile);
    }

    @Override
    public void onEditClick(VpnProfile profile) {
        editVPN(profile);
    }
}
