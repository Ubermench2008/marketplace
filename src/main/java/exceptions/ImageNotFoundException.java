package exceptions;

public class ImageNotFoundException extends RuntimeException {
    public ImageNotFoundException(String imgUrl) {
        super("Image not found: " + imgUrl);
    }
}
