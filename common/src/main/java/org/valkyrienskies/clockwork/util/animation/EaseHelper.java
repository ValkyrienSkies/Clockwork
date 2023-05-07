package org.valkyrienskies.clockwork.util.animation;

public class EaseHelper {

    /**
     * Ease In Sine + Ease Out Sine + Ease In/Out Sine
     */

    //Sine In
    public static float easeInSine(float x) {
        return (float) (1 - Math.cos((x * Math.PI) / 2));
    }
    //Sine Out
    public static float easeOutSine(float x) {
        return (float) Math.sin((x * Math.PI) / 2);
    }
    //Sine In/Out
    public static float easeInOutSine(float x) {
        return (float) (-(Math.cos(Math.PI * x) - 1) / 2);

    }

    /**
     * Ease In Quad + Ease Out Quad + Ease In/Out Quad
     */

    //Quad In
    public static float easeInQuad(float x) {
        return x * x;
    }
    //Quad Out
    public static float easeOutQuad(float x) {
        return 1 - (1 - x) * (1 - x);
    }
    //Quad In/Out
    public static float easeInOutQuad(float x) {
        return x < 0.5 ? 2 * x * x : (float) (1 - Math.pow(-2 * x + 2, 2) / 2);
    }

    /**
     * Ease In Exponential + Ease Out Exponential + Ease In/Out Exponential
     */

    //Exponential In
    public static float easeInExpo(float x) {
        return x == 0 ? 0 : (float) Math.pow(2, 10 * x - 10);
    }
    //Exponential Out
    public static float easeOutExpo(float x) {
        return x == 1 ? 1 : (float) (1 - Math.pow(2, -10 * x));
    }
    //Exponential In/Out
    public static float easeInOutExpo(float x) {
        return x == 0
                ? 0
                : x == 1
                    ? 1
                    : x < 0.5
                        ? (float) Math.pow(2, 20 * x - 10) / 2
                        : (2 - (float) Math.pow(2, -20 * x + 10)) / 2;
    }
    /**
     * Ease In Overshoot + Ease Out Overshoot + Ease In/Out Overshoot
     */

    //Overshoot In
    public static float easeInOvershoot(float x) {
        double c1 = 1.70158;
        double c3 = c1 + 1;

        return (float) (c3 * x * x * x - c1 * x * x);
    }
    //Overshoot Out
    public static float easeOutOvershoot(float x) {
        double c1 = 1.70158;
        double c3 = c1 + 1;

        return (float) (1 + c3 * Math.pow(x - 1, 3) + c1 * Math.pow(x - 1, 2));
    }
    //Overshoot In/Out
    public static float easeInOutOvershoot(float x) {
        double c1 = 1.70158;
        double c2 = c1 * 1.525;

        return x < 0.5
                ? (float) (Math.pow(2 * x, 2) * ((c2 + 1) * 2 * x - c2)) / 2
                : (float) (Math.pow(2 * x - 2, 2) * ((c2 + 1) * (x * 2 - 2) + c2) + 2) / 2;
    }

    /**
     * Ease In Elastic + Ease Out Elastic + Ease In/Out Elastic
     */

    //Elastic In
    public static float easeInElastic(float x) {
        double c4 = (2 * Math.PI) / 3;

        return x == 0
                ? 0
                : x == 1
                    ? 1
                    : (float) -(Math.pow(2, 10 * x - 10) * Math.sin((x * 10 - 10.75) * c4));
    }
    //Elastic Out
    public static float easeOutElastic(float x) {
        double c4 = (2 * Math.PI) / 3;

        return x == 0
                ? 0
                : x == 1
                    ? 1
                    : (float) (Math.pow(2, -10 * x) * Math.sin((x * 10 - 0.75) * c4) + 1);
    }
    //Elastic In/Out
    public static float easeInOutElastic(float x) {
        double c5 = (2 * Math.PI) / 4.5;

        return x == 0
                ? 0
                : x == 1
                    ? 1
                    : x < 0.5
                        ? (float) -(Math.pow(2, 20 * x - 10) * Math.sin((20 * x - 11.125) * c5)) / 2
                        : (float) (Math.pow(2, -20 * x + 10) * Math.sin((20 * x - 11.125) * c5)) / 2 + 1;
    }

    /**
     * Bounce In + Bounce Out + Bounce In/Out
     */

    //Bounce In
    public static float easeInBounce(float x) {
        return 1 - easeOutBounce(1 - x);

    }
    //Bounce Out
    public static float easeOutBounce(float x) {
        double n1 = 7.5625;
        double d1 = 2.75;

        if (x < 1 / d1) {
            return (float) (n1 * x * x);
        } else if (x < 2 / d1) {
            return (float) (n1 * (x -= 1.5 / d1) * x + 0.75);
        } else if (x < 2.5 / d1) {
            return (float) (n1 * (x -= 2.25 / d1) * x + 0.9375);
        } else {
            return (float) (n1 * (x -= 2.625 / d1) * x + 0.984375);
        }

    }
    //Bounce In/Out
    public static float easeInOutBounce(float x) {
        return x < 0.5
                ? (1 - easeOutBounce(1 - 2 * x)) / 2
                : (1 + easeOutBounce(2 * x - 1)) / 2;

    }

}
