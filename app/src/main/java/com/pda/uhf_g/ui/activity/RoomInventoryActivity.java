package com.pda.uhf_g.ui.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.LayerDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.handheld.uhfr.UHFRManager;
import com.pda.uhf_g.R;
import com.pda.uhf_g.adapter.EquipmentAdapter;
import com.pda.uhf_g.entity.Equipment;
import com.pda.uhf_g.util.CsvManager;
import com.pda.uhf_g.util.ScanUtil;
import com.pda.uhf_g.util.SharedUtil;
import com.pda.uhf_g.util.UtilSound;
import com.uhf.api.cls.Reader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RoomInventoryActivity extends AppCompatActivity {

    private Spinner spinnerRooms;
    private Spinner spinnerTypes;
    private Spinner spinnerModels;
    private AutoCompleteTextView autocompleteFindEquipment;
    private Button btnLoadCsv;
    private Button btnSaveInventory;
    private TextView tvCounterExpected;
    private TextView tvCounterRead;
    private TextView tvCounterMissing;
    private TextView tvCounterTotalReads;
    private int totalRawReads = 0;
    private RecyclerView rvEquipment;

    private Button btnScanFind;
    
    private LinearLayout layoutInventory, layoutSettings, layoutFind;
    private BottomNavigationView bottomNavigation;
    private SeekBar seekbarPower;
    private TextView tvPowerValue;
    private Button btnSaveSettings;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private boolean isFindMode = false;
    private EditText etTargetEpc;
    private Button btnSetTarget;
    private TextView tvTargetEpcDisplay;
    private ImageView ivWifiRadar;
    private TextView tvSignalPercentage;
    private String targetEpc = "";

    private EquipmentAdapter adapter;
    private List<Equipment> allEquipments = new ArrayList<>();
    private List<Equipment> currentRoomEquipments = new ArrayList<>();
    private String selectedRoom = "Todos";
    private String selectedType = "Todos";
    private String selectedModel = "Todos";

    public UHFRManager mUhfrManager;
    private ScanUtil scanUtil;
    private boolean isScanning = false;
    private SharedUtil sharedUtil;
    private KeyReceiver keyReceiver;

    private static final int MSG_EPC = 1;

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (msg.what == MSG_EPC) {
                String epc = (String) msg.obj;
                adapter.markAsFound(epc);
                totalRawReads++;
                updateCounters();
                return true;
            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_inventory);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        spinnerRooms = findViewById(R.id.spinner_rooms);
        spinnerTypes = findViewById(R.id.spinner_types);
        spinnerModels = findViewById(R.id.spinner_models);
        autocompleteFindEquipment = findViewById(R.id.autocomplete_find_equipment);
        btnLoadCsv = findViewById(R.id.btn_load_csv);
        btnSaveInventory = findViewById(R.id.btn_save_inventory);
        tvCounterExpected = findViewById(R.id.tv_counter_expected);
        tvCounterRead = findViewById(R.id.tv_counter_read);
        tvCounterMissing = findViewById(R.id.tv_counter_missing);
        tvCounterTotalReads = findViewById(R.id.tv_counter_total_reads);
        rvEquipment = findViewById(R.id.rv_equipment);
        
        layoutInventory = findViewById(R.id.layout_inventory);
        layoutSettings = findViewById(R.id.layout_settings);
        layoutFind = findViewById(R.id.layout_find);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        seekbarPower = findViewById(R.id.seekbar_power);
        tvPowerValue = findViewById(R.id.tv_power_value);
        btnSaveSettings = findViewById(R.id.btn_save_settings);

        etTargetEpc = findViewById(R.id.et_target_epc);
        btnSetTarget = findViewById(R.id.btn_set_target);
        tvTargetEpcDisplay = findViewById(R.id.tv_target_epc_display);
        ivWifiRadar = findViewById(R.id.iv_wifi_radar);
        tvSignalPercentage = findViewById(R.id.tv_signal_percentage);
        btnScanFind = findViewById(R.id.btn_scan_find);

        rvEquipment.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EquipmentAdapter(currentRoomEquipments);
        rvEquipment.setAdapter(adapter);

        btnLoadCsv.setOnClickListener(v -> loadCsvData());
        btnSaveInventory.setOnClickListener(v -> saveInventory());
        btnScanFind.setOnClickListener(v -> {
            if (isScanning) {
                stopInventory();
            } else {
                startInventory();
            }
        });

        adapter.setOnItemLongClickListener(epc -> {
            targetEpc = epc;
            tvTargetEpcDisplay.setText("Objetivo: " + targetEpc);
            etTargetEpc.setText(targetEpc);
            
            isFindMode = true;
            layoutInventory.setVisibility(View.GONE);
            layoutFind.setVisibility(View.VISIBLE);
            layoutSettings.setVisibility(View.GONE);
            bottomNavigation.setVisibility(View.GONE);
            
            Toast.makeText(this, "Etiqueta fijada para búsqueda", Toast.LENGTH_SHORT).show();
        });

        btnSetTarget.setOnClickListener(v -> {
            String epc = etTargetEpc.getText().toString().trim();
            if (!epc.isEmpty()) {
                targetEpc = epc;
                tvTargetEpcDisplay.setText("Objetivo: " + targetEpc);
                Toast.makeText(this, "Objetivo fijado", Toast.LENGTH_SHORT).show();
            }
        });

        autocompleteFindEquipment.setOnItemClickListener((parent, view, position, id) -> {
            String selection = (String) parent.getItemAtPosition(position);
            for (Equipment eq : allEquipments) {
                String eqString = getEquipmentDisplayString(eq);
                if (eqString.equals(selection)) {
                    targetEpc = eq.getEpc();
                    etTargetEpc.setText(targetEpc);
                    tvTargetEpcDisplay.setText("Objetivo: " + eq.getBrand() + " " + eq.getModel());
                    Toast.makeText(RoomInventoryActivity.this, "Objetivo fijado: " + eq.getDescription(), Toast.LENGTH_SHORT).show();
                    
                    // Hide keyboard
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(autocompleteFindEquipment.getWindowToken(), 0);
                    break;
                }
            }
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_mode_inventory) {
                isFindMode = false;
                layoutInventory.setVisibility(View.VISIBLE);
                layoutFind.setVisibility(View.GONE);
                layoutSettings.setVisibility(View.GONE);
                bottomNavigation.setVisibility(View.VISIBLE);
            } else if (item.getItemId() == R.id.nav_mode_find) {
                isFindMode = true;
                layoutInventory.setVisibility(View.GONE);
                layoutFind.setVisibility(View.VISIBLE);
                layoutSettings.setVisibility(View.GONE);
                bottomNavigation.setVisibility(View.GONE);
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        setupSettings();

        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent == spinnerRooms) selectedRoom = (String) parent.getItemAtPosition(position);
                if (parent == spinnerTypes) selectedType = (String) parent.getItemAtPosition(position);
                if (parent == spinnerModels) selectedModel = (String) parent.getItemAtPosition(position);
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerRooms.setOnItemSelectedListener(filterListener);
        spinnerTypes.setOnItemSelectedListener(filterListener);
        spinnerModels.setOnItemSelectedListener(filterListener);

        checkPermissions();
        
        UtilSound.initSoundPool(this);
    }

    private void setupSettings() {
        bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_inventory) {
                layoutInventory.setVisibility(View.VISIBLE);
                layoutSettings.setVisibility(View.GONE);
                return true;
            } else if (item.getItemId() == R.id.nav_settings) {
                layoutInventory.setVisibility(View.GONE);
                layoutSettings.setVisibility(View.VISIBLE);
                
                if (sharedUtil != null) {
                    int currentPower = sharedUtil.getPower();
                    int progress = currentPower - 5;
                    if (progress < 0) progress = 0;
                    if (progress > 28) progress = 28;
                    seekbarPower.setProgress(progress);
                    tvPowerValue.setText((progress + 5) + " dBm");
                }
                return true;
            }
            return false;
        });

        seekbarPower.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int power = progress + 5;
                tvPowerValue.setText(power + " dBm");
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnSaveSettings.setOnClickListener(v -> {
            if (mUhfrManager != null) {
                int power = seekbarPower.getProgress() + 5;
                Reader.READER_ERR err = mUhfrManager.setPower(power, power);
                if (err == Reader.READER_ERR.MT_OK_ERR) {
                    sharedUtil.savePower(power);
                    Toast.makeText(this, "Potencia configurada a " + power + " dBm", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Error al configurar la potencia", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Lector no inicializado", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
            }
        }
    }

    private void loadCsvData() {
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimetypes = {"text/csv", "text/comma-separated-values", "application/csv", "application/vnd.ms-excel"};
        intent.putExtra(android.content.Intent.EXTRA_MIME_TYPES, mimetypes);
        startActivityForResult(intent, 101);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @androidx.annotation.Nullable android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            android.net.Uri uri = data.getData();
            if (uri != null) {
                allEquipments = CsvManager.loadEquipmentsFromUri(this, uri);
                if (allEquipments.isEmpty()) {
                    Toast.makeText(this, "No se encontraron equipos o el formato es incorrecto", Toast.LENGTH_SHORT).show();
                    return;
                }

                Set<String> rooms = new HashSet<>();
                Set<String> types = new HashSet<>();
                Set<String> models = new HashSet<>();

                for (Equipment eq : allEquipments) {
                    if (eq.getRoom() != null && !eq.getRoom().trim().isEmpty()) rooms.add(eq.getRoom());
                    if (eq.getType() != null && !eq.getType().trim().isEmpty()) types.add(eq.getType());
                    if (eq.getModel() != null && !eq.getModel().trim().isEmpty()) models.add(eq.getModel());
                }

                List<String> roomList = new ArrayList<>();
                roomList.add("Todos");
                roomList.addAll(rooms);

                List<String> typeList = new ArrayList<>();
                typeList.add("Todos");
                typeList.addAll(types);

                List<String> modelList = new ArrayList<>();
                modelList.add("Todos");
                modelList.addAll(models);

                ArrayAdapter<String> roomAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roomList);
                roomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerRooms.setAdapter(roomAdapter);

                ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, typeList);
                typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerTypes.setAdapter(typeAdapter);

                ArrayAdapter<String> modelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, modelList);
                modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerModels.setAdapter(modelAdapter);

                List<String> findNames = new ArrayList<>();
                for (Equipment eq : allEquipments) {
                    findNames.add(getEquipmentDisplayString(eq));
                }
                ArrayAdapter<String> findAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, findNames);
                autocompleteFindEquipment.setAdapter(findAdapter);

                selectedRoom = "Todos";
                selectedType = "Todos";
                selectedModel = "Todos";
                applyFilters();
            }
        } else if (requestCode == 102 && resultCode == RESULT_OK && data != null) {
            android.net.Uri uri = data.getData();
            if (uri != null) {
                boolean success = CsvManager.exportInventoryResultToUri(this, currentRoomEquipments, selectedRoom, uri);
                if (success) {
                    Toast.makeText(this, "Guardado exitosamente", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Error al guardar el archivo", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void applyFilters() {
        currentRoomEquipments.clear();
        for (Equipment eq : allEquipments) {
            boolean matchRoom = selectedRoom.equals("Todos") || (eq.getRoom() != null && eq.getRoom().equals(selectedRoom));
            boolean matchType = selectedType.equals("Todos") || (eq.getType() != null && eq.getType().equals(selectedType));
            boolean matchModel = selectedModel.equals("Todos") || (eq.getModel() != null && eq.getModel().equals(selectedModel));

            if (matchRoom && matchType && matchModel) {
                eq.setFound(false);
                eq.resetReadCount();
                currentRoomEquipments.add(eq);
            }
        }
        totalRawReads = 0;
        adapter.updateData(currentRoomEquipments);
        updateCounters();
    }

    private void updateCounters() {
        int expected = currentRoomEquipments.size();
        int read = 0;
        for (Equipment eq : currentRoomEquipments) {
            if (eq.isFound()) read++;
        }
        int missing = expected - read;

        tvCounterExpected.setText(String.valueOf(expected));
        tvCounterRead.setText(String.valueOf(read));
        tvCounterMissing.setText(String.valueOf(missing));
        tvCounterTotalReads.setText(String.valueOf(totalRawReads));
    }

    private String getEquipmentDisplayString(Equipment eq) {
        return eq.getType() + " " + eq.getBrand() + " " + eq.getModel() + " (SN: " + eq.getSerialNumber() + ") - " + eq.getDescription() + " [EPC: " + eq.getEpc() + "]";
    }

    private void saveInventory() {
        if (selectedRoom.isEmpty() || currentRoomEquipments.isEmpty()) {
            Toast.makeText(this, "No hay datos para guardar", Toast.LENGTH_SHORT).show();
            return;
        }
        String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(new java.util.Date());
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
        intent.setType("application/vnd.ms-excel");
        StringBuilder filterSuffix = new StringBuilder();
        if (!selectedRoom.equals("Todos")) filterSuffix.append("_").append(selectedRoom);
        if (!selectedType.equals("Todos")) filterSuffix.append("_").append(selectedType);
        if (!selectedModel.equals("Todos")) filterSuffix.append("_").append(selectedModel);
        if (filterSuffix.length() == 0) filterSuffix.append("_Todos");

        intent.putExtra(android.content.Intent.EXTRA_TITLE, "resultado_inventario" + filterSuffix.toString() + "_" + timeStamp + ".xls");
        startActivityForResult(intent, 102);
    }

    @Override
    protected void onStart() {
        super.onStart();
        initModule();
        setScanKeyDisable();
        registerKeyCodeReceiver();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isScanning) stopInventory();
        setScanKeyEnable();
        unregisterReceiver(keyReceiver);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        closeModule();
    }

    private void initModule() {
        mUhfrManager = UHFRManager.getInstance();
        if (mUhfrManager != null) {
            sharedUtil = new SharedUtil(this);
            mUhfrManager.setPower(sharedUtil.getPower(), sharedUtil.getPower());
            mUhfrManager.setRegion(Reader.Region_Conf.valueOf(sharedUtil.getWorkFreq()));
            Toast.makeText(this, "Lector RFID inicializado", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Fallo al inicializar el lector", Toast.LENGTH_SHORT).show();
        }
    }

    private void closeModule() {
        if (mUhfrManager != null) {
            mUhfrManager.close();
            mUhfrManager = null;
        }
    }

    private void setScanKeyDisable() {
        int currentApiVersion = Build.VERSION.SDK_INT;
        if (currentApiVersion > Build.VERSION_CODES.N) {
            scanUtil = ScanUtil.getInstance(this);
            scanUtil.disableScanKey("134");
            scanUtil.disableScanKey("137");
        }
    }

    private void setScanKeyEnable() {
        int currentApiVersion = Build.VERSION.SDK_INT;
        if (currentApiVersion > Build.VERSION_CODES.N) {
            scanUtil = ScanUtil.getInstance(this);
            scanUtil.enableScanKey("134");
            scanUtil.enableScanKey("137");
        }
    }

    private void registerKeyCodeReceiver() {
        keyReceiver = new KeyReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.rfid.FUN_KEY");
        filter.addAction("android.intent.action.FUN_KEY");
        registerReceiver(keyReceiver, filter);
    }

    private class KeyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int keyCode = intent.getIntExtra("keyCode", 0);
            if (keyCode == 0) {
                keyCode = intent.getIntExtra("keycode", 0);
            }
            boolean keyDown = intent.getBooleanExtra("keydown", false);
            
            if (!keyDown) {
                if (keyCode == KeyEvent.KEYCODE_F3 || keyCode == KeyEvent.KEYCODE_F4 || keyCode == KeyEvent.KEYCODE_F7 || keyCode == 134 || keyCode == 137 || keyCode == 280) {
                    if (isScanning) {
                        stopInventory();
                    } else {
                        startInventory();
                    }
                }
            }
        }
    }

    private void startInventory() {
        if (mUhfrManager == null) return;
        mUhfrManager.setCancleInventoryFilter();
        
        if (!isFindMode) {
            mUhfrManager.asyncStartReading();
        }
        
        isScanning = true;
        
        btnScanFind.setText("DETENER BÚSQUEDA");
        btnScanFind.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F44336")));
        
        new InventoryThread().start();
    }

    private void stopInventory() {
        if (mUhfrManager == null) return;
        
        if (!isFindMode) {
            mUhfrManager.asyncStopReading();
        }
        
        isScanning = false;
        
        btnScanFind.setText("MANTENER PRESIONADO O PULSAR PARA BUSCAR");
        btnScanFind.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FF5722")));
        
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        
        runOnUiThread(() -> {
            tvSignalPercentage.setText("0%");
            if (ivWifiRadar.getDrawable() instanceof LayerDrawable) {
                LayerDrawable layerDrawable = (LayerDrawable) ivWifiRadar.getDrawable();
                ClipDrawable clipDrawable = (ClipDrawable) layerDrawable.findDrawableByLayerId(R.id.wifi_clip);
                if (clipDrawable != null) {
                    clipDrawable.setLevel(0);
                }
            }
        });
    }

    class InventoryThread extends Thread {
        private long lastBeepTime = 0;

        @Override
        public void run() {
            while (isScanning) {
                List<Reader.TAGINFO> list1;
                if (isFindMode) {
                    list1 = mUhfrManager.tagInventoryByTimer((short) 50);
                } else {
                    list1 = mUhfrManager.tagInventoryRealTime();
                }

                if (list1 != null && list1.size() > 0) {
                    for (Reader.TAGINFO tfs : list1) {
                        String epcStr = Reader.bytes_Hexstr(tfs.EpcId);
                        
                        if (isFindMode) {
                            if (epcStr.equalsIgnoreCase(targetEpc)) {
                                int rssi = tfs.RSSI;
                                int percentage = (rssi + 85) * 100 / 50;
                                if (percentage < 0) percentage = 0;
                                if (percentage > 100) percentage = 100;
                                
                                long currentTime = System.currentTimeMillis();
                                int beepInterval;
                                if (percentage < 20) {
                                    beepInterval = 1000 - (percentage * 20);
                                } else if (percentage < 60) {
                                    beepInterval = 600 - ((percentage - 20) * 10);
                                } else {
                                    beepInterval = 200 - (int)((percentage - 60) * 3.75);
                                }
                                if (beepInterval < 50) beepInterval = 50;
                                
                                if (currentTime - lastBeepTime > beepInterval) {
                                    UtilSound.play(1, 0);
                                    lastBeepTime = currentTime;
                                }
                                
                                int finalPercentage = percentage;
                                runOnUiThread(() -> {
                                    tvSignalPercentage.setText(finalPercentage + "% (Raw: " + rssi + ")");
                                    if (ivWifiRadar.getDrawable() instanceof LayerDrawable) {
                                        LayerDrawable layerDrawable = (LayerDrawable) ivWifiRadar.getDrawable();
                                        ClipDrawable clipDrawable = (ClipDrawable) layerDrawable.findDrawableByLayerId(R.id.wifi_clip);
                                        if (clipDrawable != null) {
                                            clipDrawable.setLevel(finalPercentage * 100);
                                        }
                                    }
                                });
                            }
                        } else {
                            boolean inFilter = false;
                            for (Equipment e : currentRoomEquipments) {
                                if (e.getEpc().equalsIgnoreCase(epcStr)) {
                                    inFilter = true;
                                    break;
                                }
                            }
                            if (inFilter) {
                                UtilSound.play(1, 0);
                                Message msg = handler.obtainMessage(MSG_EPC, epcStr);
                                handler.sendMessage(msg);
                            }
                        }
                    }
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
