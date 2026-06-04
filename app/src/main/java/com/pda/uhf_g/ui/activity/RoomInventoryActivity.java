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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import com.pda.uhf_g.util.ScanUtil;
import com.pda.uhf_g.util.SharedUtil;
import com.uhf.api.cls.Reader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RoomInventoryActivity extends AppCompatActivity {

    private static final String TAG = "RoomInventoryActivity";
    private Spinner spinnerRooms;
    private Button btnLoadCsv, btnSaveInventory;
    private TextView tvStatus;
    private RecyclerView rvEquipment;

    private Button btnScan;
    
    private LinearLayout layoutInventory, layoutSettings;
    private BottomNavigationView bottomNavigation;
    private SeekBar seekbarPower;
    private TextView tvPowerValue;
    private Button btnSaveSettings;

    private EquipmentAdapter adapter;
    private List<Equipment> allEquipments = new ArrayList<>();
    private List<Equipment> currentRoomEquipments = new ArrayList<>();
    private String selectedRoom = "";

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
                return true;
            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_inventory);

        spinnerRooms = findViewById(R.id.spinner_rooms);
        btnLoadCsv = findViewById(R.id.btn_load_csv);
        btnSaveInventory = findViewById(R.id.btn_save_inventory);
        tvStatus = findViewById(R.id.tv_status);
        rvEquipment = findViewById(R.id.rv_equipment);
        btnScan = findViewById(R.id.btn_scan);
        
        layoutInventory = findViewById(R.id.layout_inventory);
        layoutSettings = findViewById(R.id.layout_settings);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        seekbarPower = findViewById(R.id.seekbar_power);
        tvPowerValue = findViewById(R.id.tv_power_value);
        btnSaveSettings = findViewById(R.id.btn_save_settings);

        rvEquipment.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EquipmentAdapter(currentRoomEquipments);
        rvEquipment.setAdapter(adapter);

        btnLoadCsv.setOnClickListener(v -> loadCsvData());
        btnSaveInventory.setOnClickListener(v -> saveInventory());
        btnScan.setOnClickListener(v -> {
            if (isScanning) {
                stopInventory();
            } else {
                startInventory();
            }
        });
        
        setupSettings();

        spinnerRooms.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedRoom = (String) parent.getItemAtPosition(position);
                filterByRoom();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

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
                
                // Initialize seekbar with current power
                if (sharedUtil != null) {
                    int currentPower = sharedUtil.getPower();
                    // SeekBar max is 28. Real power ranges from 5 to 33.
                    // power = progress + 5
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
                for (Equipment eq : allEquipments) {
                    rooms.add(eq.getRoom());
                }

                List<String> roomList = new ArrayList<>(rooms);
                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roomList);
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerRooms.setAdapter(spinnerAdapter);

                if (!roomList.isEmpty()) {
                    selectedRoom = roomList.get(0);
                    filterByRoom();
                }
            }
        }
    }

    private void filterByRoom() {
        currentRoomEquipments.clear();
        for (Equipment eq : allEquipments) {
            if (eq.getRoom().equals(selectedRoom)) {
                // Reset found status when switching rooms
                eq.setFound(false);
                currentRoomEquipments.add(eq);
            }
        }
        adapter.updateData(currentRoomEquipments);
        tvStatus.setText("Habitación: " + selectedRoom + " (" + currentRoomEquipments.size() + " equipos esperados)");
    }

    private void saveInventory() {
        if (selectedRoom.isEmpty() || currentRoomEquipments.isEmpty()) {
            Toast.makeText(this, "No hay datos para guardar", Toast.LENGTH_SHORT).show();
            return;
        }
        String path = CsvManager.exportInventoryResult(currentRoomEquipments, selectedRoom);
        if (path != null) {
            Toast.makeText(this, "Guardado exitosamente en: " + path, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Error al guardar el archivo", Toast.LENGTH_SHORT).show();
        }
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
        mUhfrManager.asyncStartReading();
        isScanning = true;
        tvStatus.setText("Escaneando...");
        btnScan.setText("DETENER ESCANEO");
        btnScan.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F44336")));
        new InventoryThread().start();
    }

    private void stopInventory() {
        if (mUhfrManager == null) return;
        mUhfrManager.asyncStopReading();
        isScanning = false;
        tvStatus.setText("Escaneo detenido");
        btnScan.setText("MANTENER PRESIONADO O PULSAR PARA ESCANEAR");
        btnScan.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2196F3")));
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    class InventoryThread extends Thread {
        @Override
        public void run() {
            while (isScanning) {
                List<Reader.TAGINFO> list1 = mUhfrManager.tagInventoryRealTime();
                if (list1 != null && list1.size() > 0) {
                    UtilSound.play(1, 0);
                    for (Reader.TAGINFO tfs : list1) {
                        String epcStr = Reader.bytes_Hexstr(tfs.EpcId);
                        Message msg = handler.obtainMessage(MSG_EPC, epcStr);
                        handler.sendMessage(msg);
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
