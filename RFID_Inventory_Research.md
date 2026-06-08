# RFID Handheld Reader – Inventario de Etiquetas

## 1️⃣ ¿Cómo funciona la búsqueda (inventario) de etiquetas?

1. **Inicio** – La app llama a `tagInventoryRealTime()` o `tagInventoryByTimer()` (visto en `InventoryFragment.java` y `RoomInventoryActivity.java`).
2. **Transmisión CW** – La lectora envía una onda continua (~860‑960 MHz) que alimenta a los tags pasivos.
3. **Ronda EPC Gen2** – Se envía el comando `Query` con un **Q‑value** (0‑15) que determina la ventana de tiempo y la cantidad esperada de tags.  El algoritmo de anti‑colisión (slotted ALOHA) permite que cada tag responda en una ranura asignada.
4. **Singulación** – Si colisionan, se repite la ronda con Q‑value mayor.  Cada EPC descolisionado se devuelve en un objeto `TagInfo`.
5. **Filtrado opcional** – Se pueden aplicar filtros `Select` mediante `setCancleInventoryFilter()`.
6. **Finalización** – Al pulsar “Stop” o al caducar el temporizador se llama a `stopInventory()`, que cancela la ronda.

## 2️⃣ Potencia de transmisión en la rutina de inventario

- En el código de muestra se usa `mUhfrManager.setPower(power, power);` con `power = 30` (30 dBm ≈ 1 W).  Este valor coincide con la **potencia máxima permitida** en EE. UU (FCC) y la mayoría de regiones (ETSI).
- La potencia es **configurable**; el usuario puede cambiarla en `SettingsFragment.java` mediante `setPower()`.  El valor por defecto del demo es la máxima para maximizar el rango, pero puede bajarse para ahorrar batería o reducir lecturas no deseadas.
- La normativa es regional: EE. UU ≤ 30 dBm, EU ≤ 30 dBm EIRP (algunas regiones permiten ≈ 33 dBm).

## 3️⃣ ¿Es el RSSI el único dato utilizado?

| Dato | Disponible en la SDK | Uso en la demo |
|------|----------------------|----------------|
| **EPC** | `TagInfo.getEpc()` | Sí, es el identificador principal.
| **RSSI** | `TagInfo.getRssi()` | Se muestra en la lista (`EPCListViewAdapter`, `RecycleViewAdapter`).
| **TID** | `tagEpcTidInventoryByTimer()` devuelve EPC + TID | Disponible si se usa esa función, pero no se muestra en la UI.
| **Antena** | `TagInfo.getAntennaId()` (existe en el SDK nativo) | No expuesto en la demo.
| **Timestamp** | `TagInfo.getTimestamp()` (API interna) | No usado.
| **Phase / Doppler / Canal** | Generados por el firmware, pero **no** expuestos en la capa Java del SDK.

En conclusión, **RSSI es el único valor visualizado**, aunque el lector genera varios otros parámetros que pueden ser accedidos extendiendo la SDK.

## 4️⃣ Código de ejemplo para ajustar la potencia y leer RSSI

```java
// En SettingsFragment.java – cambiar potencia
void setPower() {
    int power = Integer.parseInt(editTextPower.getText().toString()); // 0‑30 dBm
    Reader.READER_ERR err = mainActivity.mUhfrManager.setPower(power, power);
    if (err == Reader.READER_ERR.READER_SUCCESS) {
        showToast("Power set to " + power + " dBm");
    }
}

// En InventoryFragment.java – leer RSSI de cada tag
private void inventoryEPC() {
    List<TagInfo> listTag = mainActivity.mUhfrManager.tagInventoryRealTime();
    for (TagInfo tag : listTag) {
        Log.d("Inventory", "EPC=" + tag.getEpc() + " RSSI=" + tag.getRssi());
    }
}
```

---
*Este archivo resume la investigación solicitada y está listo para ser versionado.*
