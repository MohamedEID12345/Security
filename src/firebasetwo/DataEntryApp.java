/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package firebasetwo;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.collection.LLRBNode.Color;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.*;
import java.io.FileInputStream;
import java.io.IOException;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.security.SecureRandom;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class DataEntryApp extends Application {

    private TableView<DataEntry> tableView;
    private ObservableList<DataEntry> dataEntries;
    private DatabaseReference databaseReference;
    private Timeline autoRefreshTimeline;
    private Label resultLabel;
    private String enteredText;
    private String getMac;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        initFirebase();
        initTableView();
        initAutoRefresh();

        TextField macAddressField = new TextField();
        macAddressField.setPromptText("MAC Address");
        macAddressField.setText(getMacAddress());
        macAddressField.setDisable(true);
        TextField passwordField = new TextField();
        passwordField.setPromptText("Password");
        passwordField.setText(generatePassword(8)); // Adjust the password length as needed
        passwordField.setVisible(false);
        tableView.setVisible(false);
        Button submitButton = new Button("تسجيل البيانات");
        submitButton.setOnAction(e -> {
            String macAddress = macAddressField.getText();
            String password = passwordField.getText();

            insertData(macAddress, password);
            dataEntries.add(new DataEntry(macAddress, password));

            queryMacAddress("");

            openNewStage();
            primaryStage.close();
        });
            
        resultLabel = new Label("Result will be displayed here.");

        VBox layout = new VBox(10);
        layout.setAlignment(Pos.CENTER);
        layout.getChildren().addAll(new Label("مرحباً بك"), macAddressField, passwordField, submitButton, tableView, resultLabel);
        Scene scene = new Scene(layout, 400, 400);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Start auto-refresh timeline
        autoRefreshTimeline.play();
    }

    private void initFirebase() {
        try {
            FileInputStream serviceAccount = new FileInputStream("E:\\firebasetwo\\src\\firebasetwo\\conners-ab84c-firebase-adminsdk-unj01-e59ac6adec.json");

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://conners-ab84c-default-rtdb.firebaseio.com/")
                    .build();

            FirebaseApp.initializeApp(options);
            databaseReference = FirebaseDatabase.getInstance().getReference();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initTableView() {
        tableView = new TableView<>();
        dataEntries = FXCollections.observableArrayList();

        TableColumn<DataEntry, String> macAddressColumn = new TableColumn<>("MAC Address");
        macAddressColumn.setCellValueFactory(new PropertyValueFactory<>("macAddress"));

        TableColumn<DataEntry, String> passwordColumn = new TableColumn<>("Password");
        passwordColumn.setCellValueFactory(new PropertyValueFactory<>("password"));

        tableView.getColumns().addAll(macAddressColumn, passwordColumn);
        tableView.setItems(dataEntries);

        // Auto-refresh the table when data changes in the Firebase database
        databaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
                DataEntry entry = dataSnapshot.getValue(DataEntry.class);
                dataEntries.add(entry);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String prevChildKey) {
                // Handle changes if needed
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                // Handle removal if needed
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String prevChildKey) {
                // Handle movement if needed
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle errors if needed
            }
        });
    }

    private void initAutoRefresh() {
        // Set up auto-refresh timeline (adjust the duration as needed)
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> refreshTable()));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
    }

    private void refreshTable() {
        // Clear existing data and re-fetch from Firebase
        dataEntries.clear();
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                    DataEntry entry = childSnapshot.getValue(DataEntry.class);
                    dataEntries.add(entry);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle errors if needed
            }
        });
    }

    private void insertData(String macAddress, String password) {
        DatabaseReference newDataRef = databaseReference.push();
        newDataRef.child("macAddress").setValueAsync(macAddress);
        newDataRef.child("password").setValueAsync(password);
    }

    private String getMacAddress() {
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(localhost);

            byte[] mac = networkInterface.getHardwareAddress();

            StringBuilder macAddress = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                macAddress.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
            }
            return macAddress.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private String generatePassword(int length) {
        String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();

        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(CHARACTERS.length());
            password.append(CHARACTERS.charAt(randomIndex));
        }

        return password.toString();
    }

    public static class DataEntry {

        private final String macAddress;
        private final String password;

        public DataEntry() {
            // Default constructor required for Firebase
            this.macAddress = "";
            this.password = "";
        }

        public DataEntry(String macAddress, String password) {
            this.macAddress = macAddress;
            this.password = password;
        }

        public String getMacAddress() {
            return macAddress;
        }

        public String getPassword() {
            return password;
        }
    }

    // Method to query MAC address based on password and update the label
    private void queryMacAddress(String password) {
        databaseReference.orderByChild("password").equalTo(password).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Platform.runLater(() -> {
                    try {
                        if (dataSnapshot.exists()) {
                            // Assuming there is only one result for a given password
                            for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                                DataEntry entry = childSnapshot.getValue(DataEntry.class);
                                String macAddress = entry.getMacAddress();
                                resultLabel.setText(macAddress);
                                getMac = macAddress.toString();

                                System.out.println("the value of query pass: " + getMac);
                                //open new stage
                                String realMac = getMacAddress().toString();
                                System.out.println("real mac=" + realMac);
                                System.out.println("getMac= " + getMac);
                                
                                if (getMac == null ? realMac == null : getMac.equals(realMac)) {
                                    openNewScene();
                                } 
                                else{
                                    System.out.println("NOOOOOOOOOOOOOOOOOOOOOOOOOO!");
                                     showErrorAlert("An error occurred", "غير مصرح لك بإستخدام البرنامج من فضلك تواصل مع مدير النظام.");
                                }
                            }
                        } else {
                            resultLabel.setText("No MAC Address found for password '" + password + "'.");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        // Log the exception details or handle it appropriately
                    }
                });

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle errors if needed
            }
        });
    }

    private void openNewStage() {
        
        Stage newStage = new Stage();

        // Create a text field
        TextField textField = new TextField();
        textField.setPromptText("Enter text");
        textField.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        textField.setMaxWidth(300);
        // Create a button
        Button submitButton = new Button("أدخل->");
        submitButton.setOnAction(event -> {
            enteredText = textField.getText();
            System.out.println("Entered Text: " + enteredText);
            // Add your logic here for what to do with the entered text
            DataEntryApp entry = new DataEntryApp();
            queryMacAddress(enteredText);
            newStage.close();
        });

        // Create a layout for the new stage
        VBox layout = new VBox(10);
        layout.setAlignment(Pos.CENTER);
        //layout.setStyle("-fx-background-color: #373538;");
        layout.getChildren().addAll(new Label("من فضلك أدخل الكود الذي حصلت عليه من المسؤول أو تواصل مع وأرسل له هذا الكود"), new Label(getMacAddress()),textField, submitButton);
        // Set up the new stage
        Scene newScene = new Scene(layout, 550, 200);
        //newScene.setFill(Paint.valueOf("black"));

        newStage.setScene(newScene);
        newStage.setTitle("New Stage");
        newStage.show();
    }
public void openNewScene() throws IOException {
    System.out.println("hello worled!@#$");
       URL location = getClass().getResource("new.fxml");
            System.out.println("Location of fxml: " + location);
        FXMLLoader loader = new FXMLLoader(location);
            Parent newSceneRoot = loader.load();

            Stage newStage = new Stage();
            Scene newScene = new Scene(newSceneRoot);

            newStage.setScene(newScene);
            newStage.setTitle("New Scene");
            newStage.show();
    }
private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(title);
        alert.setContentText(content);

        // Show the alert and wait for user response
        alert.showAndWait();
    }
}
