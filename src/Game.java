import org.neuroph.core.NeuralNetwork;

import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;

public class Game {

    public static final int PIPE_DELAY = 100;

    private Boolean paused;

    private int pauseDelay;
    private int restartDelay;
    private int pipeDelay;

    public static ArrayList<Bird> birds = new ArrayList<>();
    private ArrayList<Pipe> pipes;
    private Keyboard keyboard;

    public int score;
    public Boolean gameover;
    public Boolean started;

    public Game() {
        keyboard = Keyboard.getInstance();
        restart();
    }

    public void restart() {
        paused = false;
        started = false;
        gameover = false;

        score = 0;
        pauseDelay = 0;
        restartDelay = 0;
        pipeDelay = 0;
        birds.clear();
        for (int i = 0; i < App.BIRD_COUNT; i++)
            birds.add(new Bird((new File("myFlappyPerceptron.nnet").exists() ? NeuralNetwork.load("myFlappyPerceptron.nnet") : null)));
        pipes = new ArrayList<Pipe>();
    }

    public void update() {
        watchForStart();

        if (!started)
            return;

        watchForPause();
        watchForReset();

        if (paused)
            return;

        Pipe nearestPipe = null;
        for (Pipe pipe : pipes) {
            if(pipe.x < 100)
                continue;
            if(nearestPipe == null) {
                nearestPipe = pipe;
            }
            else {
                    if (nearestPipe.x > pipe.x)
                        nearestPipe = pipe;
            }
        }
        for (Bird bird : birds) {
            if(nearestPipe != null) {
                bird.horizontalLength = (nearestPipe.x /*+ nearestPipe.width*/) - 100;
                if (nearestPipe.orientation.equals("north"))
                    bird.heightDifference = (nearestPipe.y - 87.5) - bird.y;
                else
                    bird.heightDifference = (nearestPipe.y + nearestPipe.height + 87.5) - bird.y;
            }
            bird.update();
        }

        if (gameover)
            return;

        movePipes();
        checkForCollisions();
    }

    public ArrayList<Render> getRenders() {
        ArrayList<Render> renders = new ArrayList<Render>();
        renders.add(new Render(0, 0, "lib/background.png"));
        for (Pipe pipe : pipes)
            renders.add(pipe.getRender());
        renders.add(new Render(0, 0, "lib/foreground.png"));
        for (Bird bird : birds)
            renders.add(bird.getRender());
        return renders;
    }

    private void watchForStart() {
        if (!started && keyboard.isDown(KeyEvent.VK_SPACE)) {
            started = true;
        }
    }

    private void watchForPause() {
        if (pauseDelay > 0)
            pauseDelay--;

        if (keyboard.isDown(KeyEvent.VK_P) && pauseDelay <= 0) {
            paused = !paused;
            pauseDelay = 10;
        }
    }

    private void watchForReset() {
        if (restartDelay > 0)
            restartDelay--;

        if (keyboard.isDown(KeyEvent.VK_R) && restartDelay <= 0) {
            restart();
            restartDelay = 10;
            return;
        }
    }

    private void movePipes() {
        pipeDelay--;

        if (pipeDelay < 0) {
            pipeDelay = PIPE_DELAY;
            Pipe northPipe = null;
            Pipe southPipe = null;

            // Look for pipes off the screen
            for (Pipe pipe : pipes) {
                if (pipe.x - pipe.width < 0) {
                    if (northPipe == null) {
                        northPipe = pipe;
                    } else if (southPipe == null) {
                        southPipe = pipe;
                        break;
                    }
                }
            }

            if (northPipe == null) {
                Pipe pipe = new Pipe("north");
                pipes.add(pipe);
                northPipe = pipe;
            } else {
                northPipe.reset();
            }

            if (southPipe == null) {
                Pipe pipe = new Pipe("south");
                pipes.add(pipe);
                southPipe = pipe;
            } else {
                southPipe.reset();
            }

            northPipe.y = southPipe.y + southPipe.height + 175;
        }

        for (Pipe pipe : pipes) {
            pipe.update();
        }
    }

    private void checkForCollisions() {

        for (Pipe pipe : pipes) {
            for (Bird bird : birds)
                if (pipe.collides(bird.x, bird.y, bird.width, bird.height)) {
                    //gameover = true;
                    bird.dead = true;
                } else if (pipe.x == bird.x && pipe.orientation.equalsIgnoreCase("south")) {
                    score++;
                }
        }

        // Ground + Bird collision
        gameover = true;
        for (Bird bird : birds) {
            if (bird.y + bird.height > App.HEIGHT - 80) {
                bird.dead = true;
                bird.y = App.HEIGHT - 80 - bird.height;
            }
            if(!bird.dead) {
                gameover = false;
            }
        }

        if(gameover) {
            restart();
            restartDelay = 10;
            started = true;
        }

    }
}
