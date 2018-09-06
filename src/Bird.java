import org.neuroph.core.NeuralNetwork;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.util.TransferFunctionType;

import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;

public class Bird {

    public int x;
    public int y;
    public int width;
    public int height;

    public boolean dead;
    public int score = 0;

    public double yvel;
    public double gravity;

    private int jumpDelay;
    private double rotation;

    private Image image;
    private Keyboard keyboard;
    public double horizontalLength;
    public double heightDifference;


    private NeuralNetwork ann;

    public Bird(NeuralNetwork ann) {
        x = 100;
        y = 150;
        yvel = 0;
        width = 45;
        height = 32;
        gravity = 0.5;
        jumpDelay = 0;
        rotation = 0.0;
        dead = false;

        keyboard = Keyboard.getInstance();
        if(ann == null)
            ann = getNewNetwork();
        this.ann = ann;
    }

    public void update() {
        yvel += gravity;

        if (jumpDelay > 0)
            jumpDelay--;

        System.out.println("horizontalLength: "+ horizontalLength +" heightDifference: "+ heightDifference +" AI:");
        askToFlap(horizontalLength, heightDifference);
        if (!dead && keyboard.isDown(KeyEvent.VK_SPACE) && jumpDelay <= 0) {
            yvel = -10;
            jumpDelay = 10;
        }

        y += (int)yvel;
    }

    public Render getRender() {
        Render r = new Render();
        r.x = x;
        r.y = y;

        if (image == null) {
            image = Util.loadImage("lib/bird.png");     
        }
        r.image = image;

        rotation = (90 * (yvel + 20) / 20) - 90;
        rotation = rotation * Math.PI / 180;

        if (rotation > Math.PI / 2)
            rotation = Math.PI / 2;

        r.transform = new AffineTransform();
        r.transform.translate(x + width / 2, y + height / 2);
        r.transform.rotate(rotation);
        r.transform.translate(-width / 2, -height / 2);

        return r;
    }

    public void askToFlap(double horizontalLength, double heightDifference){
        ann.setInput(horizontalLength, heightDifference);
        ann.calculate();
        double[] networkOutputOne = ann.getOutput();
        System.out.println("horizontalLength: "+ horizontalLength +" heightDifference: "+ heightDifference +" AI:" + networkOutputOne[0]);
        if(networkOutputOne[0] > 0.5){
            yvel = -10;
            jumpDelay = 10;
        }

    }

    private NeuralNetwork getNewNetwork() {
        MultiLayerPerceptron myMlPerceptron = new MultiLayerPerceptron(TransferFunctionType.GAUSSIAN, 2, 6, 1);
        return myMlPerceptron;
    }

    public NeuralNetwork getAnn() {
        return ann;
    }

    public void setAnn(NeuralNetwork ann) {
        this.ann = ann;
    }
}
