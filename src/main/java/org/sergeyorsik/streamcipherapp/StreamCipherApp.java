
package org.sergeyorsik.streamcipherapp;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;

public class StreamCipherApp extends Application {
    private static final int LFSR_SIZE = 33;
    private TextField registerInput;
    private TextArea keyOutput, originalFileOutput, encryptedFileOutput;
    private Button encryptButton, decryptButton;
    private File selectedFile;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Потоковое шифрование");

        Label registerLabel = new Label("Начальное состояние регистра (33 бита):");
        registerInput = new TextField();
        registerInput.setPromptText("Введите 33 бита (только 0 и 1)");
        registerInput.textProperty().addListener((obs, oldVal, newVal) -> filterBinaryInput());

        Button fileButton = new Button("Выбрать файл");
        fileButton.setOnAction(e -> {
            try {
                selectFile(primaryStage);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        encryptButton = new Button("Зашифровать");
        encryptButton.setDisable(true);
        encryptButton.setOnAction(e -> encryptFile());

        decryptButton = new Button("Расшифровать");
        decryptButton.setDisable(true);
        decryptButton.setOnAction(e -> decryptFile());

        keyOutput = new TextArea();
        keyOutput.setEditable(false);
        keyOutput.setPromptText("Сгенерированный ключ");

        originalFileOutput = new TextArea();
        originalFileOutput.setEditable(true);
        originalFileOutput.setPromptText("Исходный файл (двоичный вид)");
        originalFileOutput.textProperty().addListener((obs, oldVal, newVal) -> filterBinaryInput());

        encryptedFileOutput = new TextArea();
        encryptedFileOutput.setEditable(false);
        encryptedFileOutput.setPromptText("Зашифрованный файл (двоичный вид)");

        VBox layout = new VBox(10, registerLabel, registerInput, fileButton, encryptButton, decryptButton,
                new Label("Ключ:"), keyOutput, new Label("Исходный файл:"), originalFileOutput,
                new Label("Зашифрованный файл:"), encryptedFileOutput);
        primaryStage.setScene(new Scene(layout, 600, 500));
        primaryStage.show();
    }

    private void filterBinaryInput() {
        String filtered = registerInput.getText().replaceAll("[^01]", "");
        if (filtered.length() > LFSR_SIZE) {
            filtered = filtered.substring(0, LFSR_SIZE);
        }
        registerInput.setText(filtered);
        System.out.println(!originalFileOutput.getText().isEmpty());
        System.out.println(filtered.length() != LFSR_SIZE || (originalFileOutput.getText().isEmpty()));
        encryptButton.setDisable(filtered.length() != LFSR_SIZE || originalFileOutput.getText().isEmpty());
        decryptButton.setDisable(filtered.length() != LFSR_SIZE || originalFileOutput.getText().isEmpty());
    }

    private void selectFile(Stage stage) throws IOException {
        FileChooser fileChooser = new FileChooser();
        selectedFile = fileChooser.showOpenDialog(stage);
        byte[] fileBytes = Files.readAllBytes(selectedFile.toPath());
        originalFileOutput.setText(formatBinaryString(bytesToBinaryString(fileBytes)));
        filterBinaryInput();
    }

    private void encryptFile() {
        processFile(true);
    }

    private void decryptFile() {
        processFile(false);
    }

    private void processFile(boolean encrypt) {
        try {
            if(originalFileOutput.getText().isEmpty()) {
                byte[] fileBytes = Files.readAllBytes(selectedFile.toPath());
                byte[] key = LFSR.generateKey(registerInput.getText(), fileBytes.length); // Генерация ключа
                byte[] result = xorBytes(fileBytes, key);

                keyOutput.setText(formatBinaryString(bytesToBinaryString(key)));
                originalFileOutput.setText(formatBinaryString(bytesToBinaryString(fileBytes)));
                encryptedFileOutput.setText(formatBinaryString(bytesToBinaryString(result)));

                File outputFile = new File(selectedFile.getParent(), (encrypt ? "encrypted" : "decrypted") + "_" + selectedFile.getName());
                Files.write(outputFile.toPath(), result);

                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Файл сохранен: " + outputFile.getAbsolutePath());
                alert.show();
            } else {
                String binaryInput = originalFileOutput.getText().replaceAll("[^01]", "");

                byte[] fileBytes = binaryStringToByteArray(binaryInput);

                byte[] key = LFSR.generateKey(registerInput.getText(), fileBytes.length);
                byte[] result = xorBytes(fileBytes, key);

                keyOutput.setText(formatBinaryString(bytesToBinaryString(key)));
                originalFileOutput.setText(formatBinaryString(binaryInput));
                encryptedFileOutput.setText(formatBinaryString(bytesToBinaryString(result)));

//                File outputFile = new File(selectedFile.getParent(), (encrypt ? "encrypted" : "decrypted") + "_" + selectedFile.getName());
//                Files.write(outputFile.toPath(), result);

//                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Файл сохранен: " + outputFile.getAbsolutePath());
//                alert.show();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] binaryStringToByteArray(String binaryString) {
        int byteCount = (binaryString.length() + 7) / 8; // Вычисляем количество байтов
        byte[] result = new byte[byteCount];

        for (int i = 0; i < binaryString.length(); i++) {
            int byteIndex = i / 8; // Определяем, в какой байт пишем
            int bitIndex = 7 - (i % 8); // Позиция бита внутри байта
            if (binaryString.charAt(i) == '1') {
                result[byteIndex] |= (1 << bitIndex); // Устанавливаем соответствующий бит
            }
        }

        return result;
    }

    private byte[] xorBytes(byte[] data, byte[] key) {
        int minLength = Math.min(data.length, key.length);
        byte[] result = new byte[data.length];
        for (int i = 0; i < minLength; i++) {
            System.out.println("File data: " + byteToBinaryString(data[i]));
            System.out.println("Key data: "+byteToBinaryString(key[i]));
            result[i] = (byte) (data[i] ^ key[i]);
            System.out.println("Result: "+byteToBinaryString(result[i]));
        }
        return result;
    }
    private String byteToBinaryString(byte b) {
        StringBuilder sb = new StringBuilder();


            for (int i = 7; i >= 0; i--) {
                boolean bit = (b & (1 << i)) != 0;
                sb.append(bit ? '1' : '0');
            }

        return sb.length() == 0 ? "0" : sb.toString();
    }

    private String bytesToBinaryString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();

        for (byte b : bytes) {
            for (int i = 7; i >= 0; i--) {
                boolean bit = (b & (1 << i)) != 0;
                sb.append(bit ? '1' : '0');

            }
        }
        return sb.length() == 0 ? "0" : sb.toString();
    }

    private String formatBinaryString(String binary) {
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < binary.length(); i++) {
            formatted.append(binary.charAt(i));
            if ((i + 1) % 8 == 0) {
                formatted.append(' ');
            }
            if ((i + 1) % 64 == 0) {
                formatted.append('\n');
            }
        }
        return formatted.toString();
    }

    public static void main(String[] args) {
        launch(args);
    }
}


class LFSR {
    private static final int[] TAPS = {33, 13};

    public static byte[] generateKey(String seed, int length) {
        byte[] key = new byte[length];
        boolean[] register = new boolean[seed.length()];

        for (int i = 0; i < seed.length(); i++) {
            register[i] = seed.charAt(i) == '1';
        }

        StringBuilder keyBits = new StringBuilder();

        System.out.println("Length: " + length);
        System.out.println("Length*8: " + length*8);

        for (int i = 0; i < length * 8; i++) { // генерируем бит за битом для ключа
            keyBits.append(register[0] ? '1' : '0');
            boolean newBit = register[0] ^ register[20];
//            for (int tap : TAPS) {111111111111111
//                newBit ^= register[tap - 1];
//            }

            System.arraycopy(register, 1, register, 0, register.length - 1);
            register[register.length - 1] = newBit;
        }

        // Преобразуем полученные биты в байты
        for (int i = 0; i < length; i++) {
            int byteStart = i * 8;
            int byteValue = Integer.parseInt(keyBits.substring(byteStart, byteStart + 8), 2);
            System.out.println("Key: " + keyBits.substring(byteStart, byteStart + 8));
            key[i] = (byte) byteValue;
        }
        System.out.println(Arrays.toString(key));

        return key;
    }
}
