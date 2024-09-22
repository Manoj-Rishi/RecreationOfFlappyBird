import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import javax.sound.sampled.AudioSystem;
import javax.swing.*;
import java.io.File;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.Clip;
public class FlappyBird extends JPanel implements ActionListener, KeyListener 
{
    int boardWidth = 480;
    int boardHeight = 860; 

    //images
    Image backgroundImg;
    Image birdImg;
    Image topPipeImg;
    Image bottomPipeImg;

    //bird class
    int birdX = boardWidth/8;
    int birdY = boardWidth/2;
    int birdWidth = 50;
    int birdHeight = 50;

    //music
    private static Clip clip;

    class Bird {
        int x = birdX;
        int y = birdY;
        int width = birdWidth;
        int height = birdHeight;
        Image img;

        Bird(Image img) {
            this.img = img;
        }
    }

    //pipe class
    int pipeX = boardWidth;
    int pipeY = 0;
    int pipeWidth = 64;  //scaled by 1/6
    int pipeHeight = 512;
    
    class Pipe {
        int x = pipeX;
        int y = pipeY;
        int width = pipeWidth;
        int height = pipeHeight;
        Image img;
        boolean passed = false;

        Pipe(Image img) {
            this.img = img;
        }
    }

    //game logic
    Bird bird;
    int velocityX = -4; //move pipes to the left speed (simulates bird moving right)
    int velocityY = 0; //move bird up/down speed.
    int gravity = 1;

    ArrayList<Pipe> pipes;
    Random random = new Random();

    Timer gameLoop;
    Timer placePipeTimer;
    boolean gameOver = false;
    double score = 0;

    JButton startButton;
    JButton restartButton;
    JButton quitButton;

    FlappyBird() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setFocusable(true);
        addKeyListener(this);

        //load images
        backgroundImg = new ImageIcon(getClass().getResource("./bg.png")).getImage();
        birdImg = new ImageIcon(getClass().getResource("./flappybird.png")).getImage();
        topPipeImg = new ImageIcon(getClass().getResource("./toppipe.png")).getImage();
        bottomPipeImg = new ImageIcon(getClass().getResource("./bottompipe.png")).getImage();

        //music

        //bird
        bird = new Bird(birdImg);
        pipes = new ArrayList<Pipe>();

        //create buttons
        startButton = new JButton("Start");
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startGame();
            }
        });

        restartButton = new JButton("Restart");
        restartButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                restartGame();
            }
        });

        quitButton = new JButton("Quit");
        quitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        //set button size
        startButton.setPreferredSize(new Dimension(150, 50));
        restartButton.setPreferredSize(new Dimension(150, 50));
        quitButton.setPreferredSize(new Dimension(150, 50));

        //add buttons to panel
        add(startButton);
        add(restartButton);
        add(quitButton);

        //set layout manager
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        add(startButton, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        add(quitButton, gbc);

        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        add(restartButton, gbc);

        restartButton.setVisible(false);
    }

    void startGame() {
        //hide start and quit buttons
        startButton.setVisible(false);
        quitButton.setVisible(false);

        //place pipes timer
        placePipeTimer = new Timer(1500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
              placePipes();
            }
        });
        placePipeTimer.start();
        playMusic("FlappyBird\\src\\bhAAi.wav");
        //game loop
        gameLoop = new Timer(1000/60, this); 
        gameLoop.start();
    }

    void restartGame() {
            restartButton.setVisible(false);
            quitButton.setVisible(false);
            //reset game conditions
            bird.y = birdY;
            velocityY = 0;
            pipes.clear();
            gameOver = false;
            score = 0;
            stopMusic();
            clip = null;

            //start game loop again
            gameLoop.start();
            placePipeTimer.start();
            playMusic("FlappyBird\\src\\bhAAi.wav");
    }

    void placePipes() {
        int randomPipeY = (int) (pipeY - pipeHeight/4 - Math.random()*(pipeHeight/2));
    
        Pipe topPipe = new Pipe(topPipeImg);
        topPipe.y = randomPipeY;
        pipes.add(topPipe);
    
        Pipe bottomPipe = new Pipe(bottomPipeImg);
        bottomPipe.y = topPipe.y  + pipeHeight + 200; // increased opening space to 200 pixels
        pipes.add(bottomPipe);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        //background
        g.drawImage(backgroundImg, 0, 0, this.boardWidth, this.boardHeight, null);

        //bird
        g.drawImage(birdImg, bird.x, bird.y, bird.width, bird.height, null);

        //pipes
        for (int i = 0; i < pipes.size(); i++) {
            Pipe pipe = pipes.get(i);
            g.drawImage(pipe.img, pipe.x, pipe.y, pipe.width, pipe.height, null);
        }

        //score
        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.PLAIN, 32));

        if (gameOver) 
        {
            String gameOverText = "Game Over: " + String.valueOf((int) score);
            FontMetrics metrics = g.getFontMetrics();
            int x = (boardWidth - metrics.stringWidth(gameOverText)) / 2; // center horizontally
            int y = boardHeight / 2 - 50; // move score up to avoid collision
            g.drawString(gameOverText, x, y);
    
            /* String restartText = "Restart";
            x = (boardWidth - metrics.stringWidth(restartText)) / 2; // center horizontally
            y = boardHeight / 2 + 20; // move restart button down to avoid collision
            g.drawString(restartText, x, y); */
        } 
        else 
        {
            g.drawString(String.valueOf((int) score), 10, 35);
        }
    }

    public void move() {
        //bird
        velocityY += gravity;
        bird.y += velocityY;
        bird.y = Math.max(bird.y, 0); //apply gravity to current bird.y, limit the bird.y to top of the canvas

        //pipes
        for (int i = 0; i < pipes.size(); i++) {
            Pipe pipe = pipes.get(i);
            pipe.x += velocityX;

            if (!pipe.passed && bird.x > pipe.x + pipe.width) {
                score += 0.5; //0.5 because there are 2 pipes! so 0.5*2 = 1, 1 for each set of pipes
                pipe.passed = true;
            }

            if (collision(bird, pipe)) {
                gameOver = true;
                stopMusic();
            }
        }

        if (bird.y > boardHeight) {
            gameOver = true;
        }
    }

    public static void playMusic(String filePath)
    {
        try {
            File music = new File(filePath);
            if(music.exists()){
                AudioInputStream audioInput = AudioSystem.getAudioInputStream(music);
                clip = AudioSystem.getClip();
                clip.open(audioInput);
                clip.start();
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            }
        }
        catch (Exception e){
            JOptionPane.showMessageDialog(null,"Error");
        }
    } 

    public static void stopMusic()
    {
        if(clip!=null)
            clip.stop();
    }

    boolean collision(Bird a, Pipe b) {
        return a.x + 5 < b.x + b.width &&   //a's top left corner doesn't reach b's top right corner
               a.x + a.width - 10 > b.x &&   //a's top right corner passes b's top left corner
               a.y +5 < b.y + b.height &&  //a's top left corner doesn't reach b's bottom left corner
               a.y + a.height - 10 > b.y;    //a's bottom left corner passes b's top left corner
    }

    @Override
    public void actionPerformed(ActionEvent e) { //called every x milliseconds by gameLoop timer
        move();
        repaint();
        if (gameOver) {
            placePipeTimer.stop();
            gameLoop.stop();
            stopMusic();
            restartButton.setVisible(true);
            quitButton.setVisible(true);
        }
    }  

    @Override
    public void keyPressed(KeyEvent e) 
    {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) 
        {
            velocityY = -9;
        }
    }
    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}
} 