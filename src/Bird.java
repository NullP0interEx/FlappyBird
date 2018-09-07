import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.data.DataSet;
import org.neuroph.core.data.DataSetRow;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.Perceptron;
import org.neuroph.nnet.learning.BackPropagation;
import org.neuroph.util.TransferFunctionType;

import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

public class Bird {

    public int x;
    public int y;
    public int width;
    public int height;

    public boolean dead;
    public boolean learn;
    public int score = 0;

    public double yvel;
    public double gravity;

    private int jumpDelay;
    private double rotation;

    private Image image;
    private Keyboard keyboard;
    public double horizontalLength;
    public double heightDifference;
    private DataSet ds = new DataSet(2, 1);
    private RingBuffer<double[]> dataBuffer = new RingBuffer<>(300);


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

        if (!dead && keyboard.isDown(KeyEvent.VK_SPACE) && jumpDelay <= 0) {
            learn = true;
            yvel = -10;
            jumpDelay = 10;
            String csv = "";
            double percentageThreshold = 0.05;
            for(int i=1; i< dataBuffer.size() + 1; i++){
                double[] dsRow = dataBuffer.pop();
                ds.add(new DataSetRow(dsRow, new double[]{( (i/(double)dataBuffer.size()) < percentageThreshold ? 0 : (i/(double)dataBuffer.size())  )}));
                csv += dsRow[0] + "," + dsRow[1] + "," + ( (i/(double)dataBuffer.size()) < percentageThreshold ? 0 : (i/(double)dataBuffer.size())  ) + "\n";
                System.out.println("horizontalLength: "+ dsRow[0] +" heightDifference: "+ dsRow[1] +" LEARN:" + ( (i/(double)dataBuffer.size()) < percentageThreshold ? 0 : (i/(double)dataBuffer.size())  ));
            }
            try {
                Files.write(Paths.get("export.txt"), csv.getBytes(), StandardOpenOption.APPEND);
            }catch (IOException e) {
                e.printStackTrace();
            }
            if(ds.size() > 0) {
                System.out.println("Start learning - elements: " + ds.size());
                BackPropagation backPropagation = new BackPropagation();
                backPropagation.setMaxIterations(10000);
                ann.learn(ds, backPropagation);
                System.out.println("End learning");
                ds.clear();
            }
        }

        if(jumpDelay <= 0)
            askToFlap(horizontalLength, heightDifference);

        if (keyboard.isDown(KeyEvent.VK_ENTER)) {

            ann.save("myFlappyPerceptron.nnet");
            System.out.println("End save");
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
        if(heightDifference != 0 && horizontalLength != 0)
            dataBuffer.push(new double[] {horizontalLength, heightDifference});
        if(!learn){
            ann.setInput(horizontalLength, heightDifference);
            ann.calculate();
            double[] networkOutputOne = ann.getOutput();
            System.out.println("horizontalLength: "+ horizontalLength +" heightDifference: "+ heightDifference +" AI:" + networkOutputOne[0]);
            if(networkOutputOne[0] > 0.8){
                yvel = -10;
                jumpDelay = 10;
            }
        }

    }

    private NeuralNetwork getNewNetwork() {
        //MultiLayerPerceptron ann = new MultiLayerPerceptron(TransferFunctionType.STEP, 2, 2*2+1, 1);
        //NeuralNetwork ann = new Perceptron(2, 1);
        return (new File("myFlappyPerceptron_backup.nnet").exists() ? NeuralNetwork.load("myFlappyPerceptron_backup.nnet") : new MultiLayerPerceptron(TransferFunctionType.STEP, 2, 2*2+1, 1));
    }

    public NeuralNetwork getAnn() {
        return ann;
    }

    public void setAnn(NeuralNetwork ann) {
        this.ann = ann;
    }
}
