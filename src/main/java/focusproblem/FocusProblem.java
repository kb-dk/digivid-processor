package focusproblem;

import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import static javafx.collections.FXCollections.observableArrayList;

public class FocusProblem extends Application {

    private TextArea notesArea;
    private TableView docTable;
    private ObservableList<Doc> docList;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    private ObservableList<Doc> initDocs() {
        docList = observableArrayList();
        docList.add(new Doc("Harper Lee", "To Kill a Mockbird",
                "Some notes on mockingbirds"));
        docList.add(new Doc("John Steinbeck", "Of Mice and Men",
                "Some notes about mice"));
        docList.add(new Doc("Lewis Carroll", "Jabberwock",
                "Some notes about jabberwocks"));
        return docList;
    }

    private Parent initGui(ObservableList<Doc> d) {
        notesArea = new TextArea();
        notesArea.setId("notesArea");
        notesArea.setPromptText("Add notes here");

        TableColumn<Doc, String> authorCol = new TableColumn<>("Author");
        authorCol.setCellValueFactory(new PropertyValueFactory<Doc, String>("author"));
        authorCol.setMinWidth(100.0d);
        TableColumn<Doc, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<Doc, String>("title"));
        titleCol.setMinWidth(250.0d);

        docTable = new TableView<>(d);
        docTable.setEditable(true);
        docTable.setPrefHeight(200.0d);
        docTable.getColumns().addAll(authorCol, titleCol);
        docTable.getSelectionModel().selectedItemProperty().addListener(new SelectionChangeListener());
        VBox vb = new VBox();
        vb.getChildren().addAll(docTable, notesArea);
        return vb;
    }

    void updateDoc(Doc d) {
        for (SimpleStringProperty ssp : d.getDirtyFieldList()) {
            System.out.println("Updating field: " + ssp.getName()
                    + " with " + ssp.getValue());
            d.markClean();
        }
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Focus Problem");
        primaryStage.setScene(new Scene(initGui(initDocs())));
        primaryStage.show();
    }

    @Override
    public void stop() {
        for (Doc d : docList) {
            updateDoc(d);
        }
    }

    public class SelectionChangeListener implements ChangeListener<Doc> {

        @Override
        public void changed(ObservableValue<? extends Doc> observable,
                            Doc oldDoc, Doc newDoc) {
            System.out.println("Changing selected row");
            if (oldDoc != null) {
                notesArea.textProperty().unbindBidirectional(oldDoc.notesProperty());
                updateDoc(oldDoc);
            }
            if (newDoc != null) {
                notesArea.setText(newDoc.getNotes());
                newDoc.notesProperty().bindBidirectional(notesArea.textProperty());
            }
        }
    }

    public class Doc {

        private final MonitoredSimpleStringProperty author;
        private final MonitoredSimpleStringProperty title;
        private final MonitoredSimpleStringProperty notes;

        public Doc(String auth, String ttl, String nts) {
            author = new MonitoredSimpleStringProperty(this, "author", auth);
            title = new MonitoredSimpleStringProperty(this, "title", ttl);
            notes = new MonitoredSimpleStringProperty(this, "notes", nts);
        }

        public String getAuthor() {
            return author.get();
        }

        public void setAuthor(String value) {
            author.set(value);
        }

        public MonitoredSimpleStringProperty authorProperty() {
            return author;
        }

        public String getTitle() {
            return title.get();
        }

        public void setTitle(String value) {
            title.set(value);
        }

        public MonitoredSimpleStringProperty titleProperty() {
            return title;
        }

        public String getNotes() {
            return notes.get();
        }

        public void setNotes(String value) {
            notes.set(value);
        }

        public MonitoredSimpleStringProperty notesProperty() {
            return notes;
        }

        public boolean isDirty() {
            return (author.isDirty() || title.isDirty() || notes.isDirty());
        }

        public ObservableList<MonitoredSimpleStringProperty> getDirtyFieldList() {
            ObservableList<MonitoredSimpleStringProperty> dirtyList = observableArrayList();
            if (author.isDirty()) {
                dirtyList.add(author);
            }
            if (title.isDirty()) {
                dirtyList.add(title);
            }
            if (notes.isDirty()) {
                dirtyList.add(notes);
            }
            return dirtyList;
        }

        public void markClean() {
            author.setDirty(false);
            title.setDirty(false);
            notes.setDirty(false);
        }
    }

    public class MonitoredSimpleStringProperty extends SimpleStringProperty {

        SimpleBooleanProperty dirty;

        public MonitoredSimpleStringProperty(Object bean, String name, String initValue) {
            super(bean, name, initValue);
            dirty = new SimpleBooleanProperty(false);
            this.addListener(new InvalidationListener() {
                @Override
                public void invalidated(Observable observable) {
                    dirty.set(true);
                }
            });
        }

        public MonitoredSimpleStringProperty(Object bean, String name) {
            this(bean, name, "");
        }

        public MonitoredSimpleStringProperty(String initialValue) {
            this(null, "");
        }

        public MonitoredSimpleStringProperty() {
            this(null, "", "");
        }

        public boolean isDirty() {
            return dirty.get();
        }

        public void setDirty(boolean newValue) {
            dirty.set(newValue);
        }
    }
}