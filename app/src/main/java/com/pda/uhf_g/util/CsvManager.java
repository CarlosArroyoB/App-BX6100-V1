package com.pda.uhf_g.util;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.pda.uhf_g.entity.Equipment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import jxl.Sheet;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

public class CsvManager {
    private static final String TAG = "CsvManager";

    public static List<Equipment> loadEquipmentsFromUri(Context context, Uri uri) {
        String displayName = getDisplayName(context, uri);
        if (displayName != null && displayName.toLowerCase().endsWith(".xls")) {
            return loadFromExcel(context, uri);
        } else {
            return loadFromCsv(context, uri);
        }
    }

    private static String getDisplayName(Context context, Uri uri) {
        android.database.Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    return cursor.getString(nameIndex);
                }
            }
        } catch (Exception e) {
            // Ignore
        } finally {
            if (cursor != null) cursor.close();
        }
        return uri.getPath();
    }

    private static List<Equipment> loadFromExcel(Context context, Uri uri) {
        List<Equipment> equipmentList = new ArrayList<>();
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            Workbook workbook = Workbook.getWorkbook(is);
            Sheet sheet = workbook.getSheet(0);
            int rows = sheet.getRows();
            for (int i = 1; i < rows; i++) { // Skip header (row 0)
                if (sheet.getColumns() >= 7) {
                    String epc = sheet.getCell(0, i).getContents().trim();
                    String itemNumber = sheet.getCell(1, i).getContents().trim();
                    String serialNumber = sheet.getCell(2, i).getContents().trim();
                    String brand = sheet.getCell(3, i).getContents().trim();
                    String model = sheet.getCell(4, i).getContents().trim();
                    
                    String type = "";
                    String room = "";
                    String description = "";
                    
                    if (sheet.getColumns() >= 8) {
                        type = sheet.getCell(5, i).getContents().trim();
                        room = sheet.getCell(6, i).getContents().trim();
                        description = sheet.getCell(7, i).getContents().trim();
                    } else {
                        // Retro-compatibility (7 columns)
                        room = sheet.getCell(5, i).getContents().trim();
                        description = sheet.getCell(6, i).getContents().trim();
                    }
                    
                    if (!epc.isEmpty()) {
                        equipmentList.add(new Equipment(epc, itemNumber, serialNumber, brand, model, type, room, description));
                    }
                }
            }
            workbook.close();
        } catch (Exception e) {
            Log.e(TAG, "Error reading Excel from Uri", e);
        }
        return equipmentList;
    }

    private static List<Equipment> loadFromCsv(Context context, Uri uri) {
        List<Equipment> equipmentList = new ArrayList<>();
        try (InputStream is = context.getContentResolver().openInputStream(uri);
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
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
                    
                    String type = "";
                    String room = "";
                    String description = "";
                    
                    if (values.length >= 8) {
                        type = values[5].trim();
                        room = values[6].trim();
                        description = values[7].trim();
                    } else {
                        room = values[5].trim();
                        description = values[6].trim();
                    }
                    equipmentList.add(new Equipment(epc, itemNumber, serialNumber, brand, model, type, room, description));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading CSV from Uri", e);
        }
        return equipmentList;
    }

    public static String exportInventoryResult(List<Equipment> equipments, String roomName, String typeName, String modelName) {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        
        StringBuilder filterSuffix = new StringBuilder();
        if (!roomName.equals("Todos")) filterSuffix.append("_").append(roomName);
        if (!typeName.equals("Todos")) filterSuffix.append("_").append(typeName);
        if (!modelName.equals("Todos")) filterSuffix.append("_").append(modelName);
        if (filterSuffix.length() == 0) filterSuffix.append("_Todos");

        String fileName = "resultado_inventario" + filterSuffix.toString() + "_" + timeStamp + ".xls";
        File resultFile = new File(downloadsDir, fileName);

        try {
            WritableWorkbook workbook = Workbook.createWorkbook(resultFile);
            WritableSheet sheet = workbook.createSheet("Inventario", 0);

            // Headers
            String[] headers = {"Codigo EPC", "Nro Item", "Numero de serie", "Marca", "Modelo", "Tipo", "Zona/Espacio", "Descripcion", "Estado", "Lecturas", "Fecha"};
            
            // Format for headers
            jxl.write.WritableFont boldFont = new jxl.write.WritableFont(jxl.write.WritableFont.ARIAL, 10, jxl.write.WritableFont.BOLD);
            jxl.write.WritableCellFormat boldFormat = new jxl.write.WritableCellFormat(boldFont);
            boldFormat.setBackground(jxl.format.Colour.GRAY_25);

            for (int i = 0; i < headers.length; i++) {
                sheet.addCell(new Label(i, 0, headers[i], boldFormat));
            }

            String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            
            for (int i = 0; i < equipments.size(); i++) {
                Equipment eq = equipments.get(i);
                int row = i + 1;
                sheet.addCell(new Label(0, row, eq.getEpc()));
                sheet.addCell(new Label(1, row, eq.getItemNumber()));
                sheet.addCell(new Label(2, row, eq.getSerialNumber()));
                sheet.addCell(new Label(3, row, eq.getBrand()));
                sheet.addCell(new Label(4, row, eq.getModel()));
                sheet.addCell(new Label(5, row, eq.getType()));
                sheet.addCell(new Label(6, row, eq.getRoom()));
                sheet.addCell(new Label(7, row, eq.getDescription()));
                sheet.addCell(new Label(8, row, eq.isFound() ? "Encontrado" : "Faltante"));
                sheet.addCell(new Label(9, row, String.valueOf(eq.getReadCount())));
                sheet.addCell(new Label(10, row, currentDate));
            }

            workbook.write();
            workbook.close();
            return resultFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Error exporting Excel", e);
            return null;
        }
    }

    public static boolean exportInventoryResultToUri(Context context, List<Equipment> equipments, String roomName, Uri uri) {
        try (java.io.OutputStream os = context.getContentResolver().openOutputStream(uri)) {
            WritableWorkbook workbook = Workbook.createWorkbook(os);
            WritableSheet sheet = workbook.createSheet("Inventario", 0);

            // Headers
            String[] headers = {"Codigo EPC", "Nro Item", "Numero de serie", "Marca", "Modelo", "Tipo", "Zona/Espacio", "Descripcion", "Estado", "Lecturas", "Fecha"};
            
            // Format for headers
            jxl.write.WritableFont boldFont = new jxl.write.WritableFont(jxl.write.WritableFont.ARIAL, 10, jxl.write.WritableFont.BOLD);
            jxl.write.WritableCellFormat boldFormat = new jxl.write.WritableCellFormat(boldFont);
            boldFormat.setBackground(jxl.format.Colour.GRAY_25);

            for (int i = 0; i < headers.length; i++) {
                sheet.addCell(new Label(i, 0, headers[i], boldFormat));
            }

            String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            
            for (int i = 0; i < equipments.size(); i++) {
                Equipment eq = equipments.get(i);
                int row = i + 1;
                sheet.addCell(new Label(0, row, eq.getEpc()));
                sheet.addCell(new Label(1, row, eq.getItemNumber()));
                sheet.addCell(new Label(2, row, eq.getSerialNumber()));
                sheet.addCell(new Label(3, row, eq.getBrand()));
                sheet.addCell(new Label(4, row, eq.getModel()));
                sheet.addCell(new Label(5, row, eq.getType()));
                sheet.addCell(new Label(6, row, eq.getRoom()));
                sheet.addCell(new Label(7, row, eq.getDescription()));
                sheet.addCell(new Label(8, row, eq.isFound() ? "Encontrado" : "Faltante"));
                sheet.addCell(new Label(9, row, String.valueOf(eq.getReadCount())));
                sheet.addCell(new Label(10, row, currentDate));
            }

            workbook.write();
            workbook.close();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error exporting Excel to Uri", e);
            return false;
        }
    }
}
