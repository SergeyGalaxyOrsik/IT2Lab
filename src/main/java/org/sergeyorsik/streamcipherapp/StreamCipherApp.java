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
        fileButton.setOnAction(e -> selectFile(primaryStage));

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
        originalFileOutput.setEditable(false);
        originalFileOutput.setPromptText("Исходный файл (двоичный вид)");

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
        encryptButton.setDisable(filtered.length() != LFSR_SIZE || selectedFile == null);
        decryptButton.setDisable(filtered.length() != LFSR_SIZE || selectedFile == null);
    }

    private void selectFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        selectedFile = fileChooser.showOpenDialog(stage);
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
            byte[] fileBytes = Files.readAllBytes(selectedFile.toPath());
            byte[] key = LFSR.generateKey(registerInput.getText(), fileBytes.length);
            byte[] result = xorBytes(fileBytes, key);

            keyOutput.setText(bytesToBinaryString(key));
            originalFileOutput.setText(bytesToBinaryString(fileBytes));
            encryptedFileOutput.setText(bytesToBinaryString(result));

            File outputFile = new File(selectedFile.getParent(), (encrypt ? "encrypted" : "decrypted") + "_" + selectedFile.getName());
            Files.write(outputFile.toPath(), result);

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Файл сохранен: " + outputFile.getAbsolutePath());
            alert.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] xorBytes(byte[] data, byte[] key) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
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

    public static void main(String[] args) {
        launch(args);
    }
}

class LFSR {
    private static final int[] TAPS = {33, 13, 12, 10};

    public static byte[] generateKey(String seed, int length) {
        byte[] key = new byte[length];
        boolean[] register = new boolean[seed.length()];
        for (int i = 0; i < seed.length(); i++) {
            register[i] = seed.charAt(i) == '1';
        }

        for (int i = 0; i < length; i++) {
            key[i] = (byte) (register[0] ? 1 : 0);
            boolean newBit = false;
            for (int tap : TAPS) {
                newBit ^= register[tap - 1];
            }
            System.arraycopy(register, 1, register, 0, register.length - 1);
            register[register.length - 1] = newBit;
        }
        return key;
    }
}
