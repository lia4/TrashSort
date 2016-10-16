import java.io.File;
import java.util.List;
import clarifai2.api.*;


public class PhotoChecker {
    private File photo;
    private final ClarifaiClient client;

    public PhotoChecker(File photo) {
        this.photo = photo;
        this.client = new ClarifaiBuilder("BZ8FbcALIw8rIup0pJVbflD3E8jpxPML344krhh4", "PqJozs2-fcxMC0Le0Xe6K677OOVozx9CDqWeyQqP").buildSync();
    }

    public List<RecognitionResult> getResults() {
        return client.recognize(new RecognitionRequest("can.jpg"));
    }

}
