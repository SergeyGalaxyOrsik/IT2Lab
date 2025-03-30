module org.sergeyorsik.streamcipherapp {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.sergeyorsik.streamcipherapp to javafx.fxml;
    exports org.sergeyorsik.streamcipherapp;
}