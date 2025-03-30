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
        encryptButton.setDisable(filtered.length() != LFSR_SIZE || originalFileOutput.getText().isEmpty() || selectedFile == null);
        decryptButton.setDisable(filtered.length() != LFSR_SIZE || originalFileOutput.getText().isEmpty() || selectedFile == null);
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
                byte[] key = LFSR.generateKey(registerInput.getText(), fileBytes.length); // исправление здесь
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

                File outputFile = new File(selectedFile.getParent(), (encrypt ? "encrypted" : "decrypted") + "_" + selectedFile.getName());
                Files.write(outputFile.toPath(), result);

                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Файл сохранен: " + outputFile.getAbsolutePath());
                alert.show();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] binaryStringToByteArray(String binaryString) {
        int len = binaryString.length();
        byte[] byteArray = new byte[(len + 7) / 8];
        for (int i = 0; i < len; i += 8) {
            byte b = 0;
            for (int j = 0; j < 8 && (i + j) < len; j++) {
                b |= (binaryString.charAt(i + j) - '0') << (7 - j);
            }
            byteArray[i / 8] = b;
        }
        return byteArray;
    }

    private byte[] xorBytes(byte[] data, byte[] key) {
        int minLength = Math.min(data.length, key.length);
        byte[] result = new byte[data.length];
        for (int i = 0; i < minLength; i++) {
            result[i] = (byte) (data[i] ^ key[i]);
        }
        return result;
    }

    private String bytesToBinaryString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        }
        return sb.toString();
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

        for (int i = 0; i < length; i++) {
            key[i] = (byte) (register[0] ? 1 : 0);

            System.out.print("Step " + (i + 1) + ": ");
            for (boolean bit : register) {
                System.out.print(bit ? "1" : "0");
            }
            System.out.println();

            boolean newBit = false;
            for (int tap : TAPS) {
                newBit ^= register[tap - 1];
            }

            System.arraycopy(register, 1, register, 0, register.length - 1);
            register[register.length - 1] = newBit;
        }

        System.out.println("Generated Key: " + Arrays.toString(key));

        return key;
    }
}
