package com.pda.uhf_g.util;

import android.os.Environment;
import android.util.Log;

import com.pda.uhf_g.entity.Equipment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CsvManager {
    private static final String TAG = "CsvManager";

    public static List<Equipment> loadEquipmentsFromUri(android.content.Context context, android.net.Uri uri) {
        List<Equipment> equipmentList = new ArrayList<>();
        
        try (java.io.InputStream is = context.getContentResolver().openInputStream(uri);
             BufferedReader br = new BufferedReader(new java.io.InputStreamReader(is))) {
            String line;
            boolean isFirstLine = true;
            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false; // Skip header
                    continue;
                }
                String[] values = line.split(",");
                if (values.length >= 7) {
                    String epc = values[0].trim();
                    String itemNumber = values[1].trim();
                    String serialNumber = values[2].trim();
                    String brand = values[3].trim();
                    String model = values[4].trim();
                    String room = values[5].trim();
                    String description = values[6].trim();
                    equipmentList.add(new Equipment(epc, itemNumber, serialNumber, brand, model, room, description));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading CSV from Uri", e);
        }

        return equipmentList;
    }

    public static String exportInventoryResult(List<Equipment> equipments, String roomName) {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "resultado_inventario_" + roomName + "_" + timeStamp + ".csv";
        File resultFile = new File(downloadsDir, fileName);

        try (FileWriter fw = new FileWriter(resultFile)) {
            fw.append("Codigo EPC,Nro Item,Numero de serie,Marca,Modelo,Habitacion,Descripcion,Estado,Fecha\n");
            String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            
            for (Equipment eq : equipments) {
                fw.append(eq.getEpc()).append(",");
                fw.append(eq.getItemNumber()).append(",");
                fw.append(eq.getSerialNumber()).append(",");
                fw.append(eq.getBrand()).append(",");
                fw.append(eq.getModel()).append(",");
                fw.append(eq.getRoom()).append(",");
                fw.append(eq.getDescription()).append(",");
                fw.append(eq.isFound() ? "Encontrado" : "Faltante").append(",");
                fw.append(currentDate).append("\n");
            }
            fw.flush();
            return resultFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Error exporting CSV", e);
            return null;
        }
    }
}
