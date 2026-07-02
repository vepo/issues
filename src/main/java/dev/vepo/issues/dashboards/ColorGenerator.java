package dev.vepo.issues.dashboards;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Stream;

public class ColorGenerator {
    private int size;
    private int currentIndex;
    private int[][] colors;
    private Random random;

    // Construtor que recebe o tamanho do array
    public ColorGenerator(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("O tamanho deve ser maior que zero");
        }

        this.size = size;
        this.currentIndex = 0;
        this.colors = new int[size][3]; // [R, G, B]
        this.random = new SecureRandom();

        gerarCores();
    }

    // Gera cores "legais" - cores mais suaves e agradáveis
    private void gerarCores() {
        // Paleta de cores pré-definidas consideradas "legais"
        int[][] paletaBase = { { 255, 107, 107 }, // Coral
            { 255, 206, 107 }, // Laranja suave
            { 255, 255, 107 }, // Amarelo claro
            { 107, 255, 107 }, // Verde menta
            { 107, 255, 255 }, // Azul claro
            { 107, 107, 255 }, // Azul royal
            { 206, 107, 255 }, // Roxo
            { 255, 107, 206 }, // Rosa
            { 72, 149, 239 }, // Azul vibrante
            { 34, 193, 195 }, // Turquesa
            { 253, 187, 45 }, // Dourado
            { 131, 56, 236 }, // Roxo profundo
            { 58, 134, 255 }, // Azul elétrico
            { 255, 94, 77 }, // Vermelho coral
            { 29, 209, 161 } // Verde esmeralda
        };

        // Se o tamanho solicitado for menor ou igual à paleta base, usa as cores
        // diretamente
        if (size <= paletaBase.length) {
            System.arraycopy(paletaBase, 0, colors, 0, size);
        } else {
            // Preenche com a paleta base primeiro
            System.arraycopy(paletaBase, 0, colors, 0, paletaBase.length);

            // Gera cores adicionais baseadas na paleta existente com variações
            for (int i = paletaBase.length; i < size; i++) {
                int[] corBase = paletaBase[random.nextInt(paletaBase.length)];
                colors[i] = gerarVariacao(corBase);
            }
        }
    }

    // Gera uma variação de uma cor base
    private int[] gerarVariacao(int[] corBase) {
        int[] variacao = new int[3];

        // Varia cada componente em até ±30, mantendo dentro do range 0-255
        for (int i = 0; i < 3; i++) {
            int novoValor = corBase[i] + random.nextInt(61) - 30; // -30 a +30
            variacao[i] = Math.clamp(novoValor, 0, 255);
        }

        return variacao;
    }

    // Retorna a próxima cor no array
    public String next() {
        if (currentIndex >= size) {
            // Se chegou ao final, reinicia do começo
            currentIndex = 0;
        }

        return asHexColor(colors[currentIndex++]);
    }

    private static String asHexColor(int[] color) {
        return String.format("#%02X%02X%02X", color[0], color[1], color[2]);
    }

    public static String[] asArray(int size) {
        var generator = new ColorGenerator(size);
        return Stream.of(generator.colors)
                     .map(ColorGenerator::asHexColor)
                     .toArray(String[]::new);
    }

}