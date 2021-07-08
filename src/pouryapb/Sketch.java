package pouryapb;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;

public class Sketch extends JFrame {

	private static final long serialVersionUID = -7044496989599732077L;
	private static final int WIDTH = 1250;
	private static final int HEIGHT = WIDTH / 4 * 3;
	private DrawCanvas canvas;
	private BufferedImage image;
	private BufferedImage dither;

	private class DrawCanvas extends JPanel {

		private static final long serialVersionUID = 8193212504627188925L;

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);

			((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			setBackground(Color.decode("#212121"));

			if (image != null) {

				Image scaledImage = image.getScaledInstance(-1, HEIGHT / 2 - 20, BufferedImage.TYPE_INT_ARGB);
				Image scaledDither = dither.getScaledInstance(-1, HEIGHT / 2 - 20, BufferedImage.TYPE_INT_ARGB);

				g.drawImage(scaledImage, WIDTH / 2 - (scaledImage.getWidth(null) / 2),
						HEIGHT / 2 - scaledImage.getHeight(null) - 10, null);
				g.drawImage(scaledDither, WIDTH / 2 - (scaledDither.getWidth(null) / 2), HEIGHT / 2 + 10, null);
			} else {
				g.setColor(Color.white);
				g.drawString("MAYBE PROCESSING...", WIDTH / 2, HEIGHT / 2);
			}
		}
	}

	public Sketch() {

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e1) {
			e1.printStackTrace();
		}

		canvas = new DrawCanvas();
		setContentPane(canvas);

		setPreferredSize(new Dimension(WIDTH, HEIGHT));
		setTitle("Dithering");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
		setResizable(false);
		setLocationRelativeTo(null);
		setFocusable(true);
		requestFocusInWindow();
		setVisible(true);

		dithering();
	}

	private void dithering() {
		image = loadImage();
		dither = new BufferedImage(image.getColorModel(), image.copyData(null), image.isAlphaPremultiplied(), null);
		dither = filterBW(dither);

		int factor = 4;

		for (int y = 0; y < dither.getHeight() - 1; y++) {
			for (int x = 1; x < dither.getWidth() - 1; x++) {

				Color pixel = new Color(dither.getRGB(x, y));

				int oldR = pixel.getRed();
				int oldG = pixel.getGreen();
				int oldB = pixel.getBlue();

				int newR = findClosestPalleteColor(oldR, factor);
				int newG = findClosestPalleteColor(oldG, factor);
				int newB = findClosestPalleteColor(oldB, factor);

				int errR = oldR - newR;
				int errG = oldG - newG;
				int errB = oldB - newB;

				dither.setRGB(x, y, new Color(newR, newG, newB).getRGB());

				try {
					dither.setRGB(x + 1, y,
							(quantize(new Color(dither.getRGB(x + 1, y)), errR, errG, errB, 7)).getRGB());
					dither.setRGB(x - 1, y + 1,
							(quantize(new Color(dither.getRGB(x - 1, y + 1)), errR, errG, errB, 3)).getRGB());
					dither.setRGB(x, y + 1,
							(quantize(new Color(dither.getRGB(x, y + 1)), errR, errG, errB, 5)).getRGB());
					dither.setRGB(x + 1, y + 1,
							(quantize(new Color(dither.getRGB(x + 1, y + 1)), errR, errG, errB, 1)).getRGB());
				} catch (Exception e) {
				}

			}
		}

		writeImage();

		repaint();
	}

	private BufferedImage loadImage() {

		var chooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
		var filter = new FileNameExtensionFilter("JPG & PNG Images", "jpg", "png");
		chooser.setFileFilter(filter);
		int returnVal = chooser.showOpenDialog(null);

		BufferedImage img = null;
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			try {
				img = ImageIO.read(chooser.getSelectedFile());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return img;
	}

	private boolean writeImage() {

		var chooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
		var filter = new FileNameExtensionFilter("JPG Images", "jpg");
		chooser.setFileFilter(filter);
		int returnVal = chooser.showSaveDialog(null);

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			try {
				ImageIO.write(dither, "jpg",
						chooser.getSelectedFile().getPath().endsWith(".jpg") ? chooser.getSelectedFile()
								: new File(chooser.getSelectedFile().getPath() + ".jpg"));
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return false;
	}

	private int findClosestPalleteColor(int val, int factor) {
		return (factor * val / 255) * (255 / factor);
	}

	private Color quantize(Color pixel, int errR, int errG, int errB, int p) {

		int r = pixel.getRed();
		int g = pixel.getGreen();
		int b = pixel.getBlue();

		r = r + errR * p / 16;
		g = g + errG * p / 16;
		b = b + errB * p / 16;

		return new Color(r, g, b);
	}

	protected BufferedImage filterBW(BufferedImage image) {

		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {

				Color color = new Color(image.getRGB(x, y));

				int r = color.getRed();
				int g = color.getGreen();
				int b = color.getBlue();

				r = g = b = (r + g + b) / 3;

				color = new Color(r, g, b);

				image.setRGB(x, y, color.getRGB());
			}
		}

		return image;
	}

	public static void main(String[] args) {

		SwingUtilities.invokeLater(() -> new Sketch());
	}
}
