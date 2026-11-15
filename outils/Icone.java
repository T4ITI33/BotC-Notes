import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Icône pixel art 32x32 du bloc-notes, agrandie en 512x512. Usage : java Icone.java <sortie.png> */
public class Icone {
    static int[][] g = new int[32][32];
    static void px(int x, int y, int c) { if (x >= 0 && y >= 0 && x < 32 && y < 32) g[y][x] = c; }
    static void rect(int x1, int y1, int x2, int y2, int c) { for (int y = y1; y <= y2; y++) for (int x = x1; x <= x2; x++) px(x, y, c); }

    public static void main(String[] a) throws Exception {
        // fond : carré bordeaux aux coins arrondis
        rect(0, 0, 31, 31, 0xFF3A1414);
        rect(1, 1, 30, 30, 0xFF4A1A1A);
        int[][] coins = {{0,0},{1,0},{0,1},{31,0},{30,0},{31,1},{0,31},{1,31},{0,30},{31,31},{30,31},{31,30}};
        for (int[] c : coins) px(c[0], c[1], 0);
        px(1,1,0xFF3A1414); px(30,1,0xFF3A1414); px(1,30,0xFF3A1414); px(30,30,0xFF3A1414);

        // ombre de la note
        rect(6, 6, 26, 27, 0xFF2A0C0C);
        // la note : cadre, fond sombre, barre de titre rouge
        rect(4, 4, 25, 26, 0xFF6E5E4E);
        rect(5, 5, 24, 25, 0xFF141414);
        rect(5, 5, 24, 8, 0xFF8A2424);
        rect(5, 9, 24, 9, 0xFF5A1A1A);
        rect(7, 6, 13, 7, 0xFFE8C9A0);           // « Notes »
        // lignes de texte
        int[] longueurs = {15, 11, 14, 8, 12};
        for (int i = 0; i < longueurs.length; i++) rect(7, 12 + i * 3, 7 + longueurs[i] - 1, 12 + i * 3, 0xFFD8D0C4);
        // poignée de redimensionnement
        px(24, 23, 0xFFB0B0B0); px(23, 24, 0xFFB0B0B0); px(24, 24, 0xFFB0B0B0); px(22, 25, 0xFFB0B0B0); px(23, 25, 0xFFB0B0B0); px(24, 25, 0xFFB0B0B0);

        // crayon en diagonale
        double ax = 28.5, ay = 9.5, bx = 15.5, by = 22.5;
        for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) {
            double cx = x + 0.5, cy = y + 0.5;
            double dx = bx - ax, dy = by - ay, len2 = dx * dx + dy * dy;
            double t = Math.max(0, Math.min(1, ((cx - ax) * dx + (cy - ay) * dy) / len2));
            double d = Math.hypot(cx - (ax + t * dx), cy - (ay + t * dy));
            double r = t > 0.78 ? 1.6 * (1 - t) / 0.22 : 1.6;
            if (d <= r) {
                int c;
                if (t < 0.12) c = 0xFFD06070;            // gomme
                else if (t < 0.18) c = 0xFFB8B8B8;       // virole
                else if (t < 0.78) c = (d < 0.6 ? 0xFFF2CC60 : 0xFFD9A93A); // corps
                else if (t < 0.93) c = 0xFFE8C9A0;       // bois
                else c = 0xFF303030;                     // mine
                px(x, y, c);
            } else if (d <= r + 0.9 && t > 0.0 && t < 0.97) {
                px(x, y, 0xFF1E0E06);                    // contour
            }
        }

        int s = 16;
        BufferedImage img = new BufferedImage(32 * s, 32 * s, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 32 * s; y++) for (int x = 0; x < 32 * s; x++) img.setRGB(x, y, g[y / s][x / s]);
        ImageIO.write(img, "png", new File(a[0]));
    }
}
