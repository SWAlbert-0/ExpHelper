package example.moo.nsga2.solver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class Nsga2Zdt1Solver {
    public static class Config {
        public int nVars = 100;
        public int populationSize = 200;
        public int maxGenerations = 250;
        public double crossoverProbability = 0.9;
        public double mutationProbability = 0.01;
        public long seed = 20260223L;
        public int maxReturnedPoints = 80;
    }

    public static class Point {
        public final double f1;
        public final double f2;

        public Point(double f1, double f2) {
            this.f1 = f1;
            this.f2 = f2;
        }
    }

    public List<Point> solve(Config config) {
        Random random = new Random(config.seed);
        List<Solution> population = initPopulation(config, random);

        for (int gen = 0; gen < config.maxGenerations; gen++) {
            evaluateAll(population, config.nVars);
            assignRankAndCrowding(population);
            List<Solution> offspring = reproduce(population, config, random);
            evaluateAll(offspring, config.nVars);

            List<Solution> union = new ArrayList<>(population.size() + offspring.size());
            union.addAll(population);
            union.addAll(offspring);
            population = environmentalSelection(union, config.populationSize);
        }

        evaluateAll(population, config.nVars);
        List<List<Solution>> fronts = fastNonDominatedSort(population);
        List<Solution> firstFront = fronts.isEmpty() ? new ArrayList<>() : fronts.get(0);
        firstFront.sort(Comparator.comparingDouble(s -> s.f1));

        int cap = Math.min(config.maxReturnedPoints, firstFront.size());
        List<Point> out = new ArrayList<>(cap);
        for (int i = 0; i < cap; i++) {
            Solution s = firstFront.get(i);
            out.add(new Point(s.f1, s.f2));
        }
        return out;
    }

    private List<Solution> initPopulation(Config config, Random random) {
        List<Solution> pop = new ArrayList<>(config.populationSize);
        for (int i = 0; i < config.populationSize; i++) {
            Solution s = new Solution(config.nVars);
            for (int j = 0; j < config.nVars; j++) {
                s.x[j] = random.nextDouble();
            }
            pop.add(s);
        }
        return pop;
    }

    private List<Solution> reproduce(List<Solution> population, Config config, Random random) {
        List<Solution> offspring = new ArrayList<>(config.populationSize);
        while (offspring.size() < config.populationSize) {
            Solution p1 = tournament(population, random);
            Solution p2 = tournament(population, random);
            Solution[] children = sbxCrossover(p1, p2, config, random);
            polynomialMutation(children[0], config, random);
            polynomialMutation(children[1], config, random);
            offspring.add(children[0]);
            if (offspring.size() < config.populationSize) {
                offspring.add(children[1]);
            }
        }
        return offspring;
    }

    private Solution tournament(List<Solution> population, Random random) {
        Solution a = population.get(random.nextInt(population.size()));
        Solution b = population.get(random.nextInt(population.size()));
        if (a.rank < b.rank) return a;
        if (b.rank < a.rank) return b;
        if (a.crowdingDistance > b.crowdingDistance) return a;
        if (b.crowdingDistance > a.crowdingDistance) return b;
        return random.nextBoolean() ? a : b;
    }

    private Solution[] sbxCrossover(Solution p1, Solution p2, Config config, Random random) {
        final double etaC = 20.0;
        Solution c1 = p1.copy();
        Solution c2 = p2.copy();
        if (random.nextDouble() > config.crossoverProbability) {
            return new Solution[]{c1, c2};
        }
        for (int i = 0; i < config.nVars; i++) {
            double y1 = p1.x[i];
            double y2 = p2.x[i];
            if (Math.abs(y1 - y2) < 1e-12) {
                continue;
            }
            double xl = 0.0;
            double xu = 1.0;
            double u = random.nextDouble();
            double beta = 1.0 + (2.0 * Math.min(y1 - xl, y2 - xl) / Math.abs(y2 - y1));
            double alpha = 2.0 - Math.pow(beta, -(etaC + 1.0));
            double betaq;
            if (u <= 1.0 / alpha) {
                betaq = Math.pow(u * alpha, 1.0 / (etaC + 1.0));
            } else {
                betaq = Math.pow(1.0 / (2.0 - u * alpha), 1.0 / (etaC + 1.0));
            }
            double child1 = 0.5 * ((y1 + y2) - betaq * Math.abs(y2 - y1));

            beta = 1.0 + (2.0 * Math.min(xu - y1, xu - y2) / Math.abs(y2 - y1));
            alpha = 2.0 - Math.pow(beta, -(etaC + 1.0));
            if (u <= 1.0 / alpha) {
                betaq = Math.pow(u * alpha, 1.0 / (etaC + 1.0));
            } else {
                betaq = Math.pow(1.0 / (2.0 - u * alpha), 1.0 / (etaC + 1.0));
            }
            double child2 = 0.5 * ((y1 + y2) + betaq * Math.abs(y2 - y1));

            c1.x[i] = clamp(child1, xl, xu);
            c2.x[i] = clamp(child2, xl, xu);
        }
        return new Solution[]{c1, c2};
    }

    private void polynomialMutation(Solution s, Config config, Random random) {
        final double etaM = 20.0;
        for (int i = 0; i < config.nVars; i++) {
            if (random.nextDouble() > config.mutationProbability) {
                continue;
            }
            double y = s.x[i];
            double yl = 0.0;
            double yu = 1.0;
            double delta1 = (y - yl) / (yu - yl);
            double delta2 = (yu - y) / (yu - yl);
            double rnd = random.nextDouble();
            double mutPow = 1.0 / (etaM + 1.0);
            double deltaq;
            if (rnd <= 0.5) {
                double xy = 1.0 - delta1;
                double val = 2.0 * rnd + (1.0 - 2.0 * rnd) * Math.pow(xy, etaM + 1.0);
                deltaq = Math.pow(val, mutPow) - 1.0;
            } else {
                double xy = 1.0 - delta2;
                double val = 2.0 * (1.0 - rnd) + 2.0 * (rnd - 0.5) * Math.pow(xy, etaM + 1.0);
                deltaq = 1.0 - Math.pow(val, mutPow);
            }
            y = y + deltaq * (yu - yl);
            s.x[i] = clamp(y, yl, yu);
        }
    }

    private List<Solution> environmentalSelection(List<Solution> union, int targetSize) {
        evaluateAll(union, union.get(0).x.length);
        assignRankAndCrowding(union);
        List<List<Solution>> fronts = fastNonDominatedSort(union);

        List<Solution> next = new ArrayList<>(targetSize);
        for (List<Solution> front : fronts) {
            assignCrowdingDistance(front);
            if (next.size() + front.size() <= targetSize) {
                next.addAll(front);
            } else {
                front.sort((a, b) -> Double.compare(b.crowdingDistance, a.crowdingDistance));
                int remain = targetSize - next.size();
                for (int i = 0; i < remain; i++) {
                    next.add(front.get(i));
                }
                break;
            }
        }
        return next;
    }

    private void evaluateAll(List<Solution> pop, int nVars) {
        for (Solution s : pop) {
            evaluateZdt1(s, nVars);
        }
    }

    private void evaluateZdt1(Solution s, int nVars) {
        double f1 = s.x[0];
        double sum = 0.0;
        for (int i = 1; i < nVars; i++) {
            sum += s.x[i];
        }
        double g = 1.0 + 9.0 * sum / (nVars - 1);
        double h = 1.0 - Math.sqrt(f1 / g);
        double f2 = g * h;
        s.f1 = f1;
        s.f2 = f2;
    }

    private void assignRankAndCrowding(List<Solution> pop) {
        List<List<Solution>> fronts = fastNonDominatedSort(pop);
        for (int i = 0; i < fronts.size(); i++) {
            List<Solution> front = fronts.get(i);
            for (Solution s : front) {
                s.rank = i;
            }
            assignCrowdingDistance(front);
        }
    }

    private List<List<Solution>> fastNonDominatedSort(List<Solution> pop) {
        List<List<Solution>> fronts = new ArrayList<>();
        List<Solution> first = new ArrayList<>();

        for (Solution p : pop) {
            p.dominationCount = 0;
            p.dominated.clear();
            for (Solution q : pop) {
                if (p == q) continue;
                if (dominates(p, q)) {
                    p.dominated.add(q);
                } else if (dominates(q, p)) {
                    p.dominationCount++;
                }
            }
            if (p.dominationCount == 0) {
                first.add(p);
            }
        }
        fronts.add(first);

        int i = 0;
        while (i < fronts.size() && !fronts.get(i).isEmpty()) {
            List<Solution> next = new ArrayList<>();
            for (Solution p : fronts.get(i)) {
                for (Solution q : p.dominated) {
                    q.dominationCount--;
                    if (q.dominationCount == 0) {
                        next.add(q);
                    }
                }
            }
            i++;
            if (!next.isEmpty()) {
                fronts.add(next);
            }
        }
        return fronts;
    }

    private boolean dominates(Solution a, Solution b) {
        boolean betterInAny = false;
        if (a.f1 > b.f1 || a.f2 > b.f2) {
            return false;
        }
        if (a.f1 < b.f1) betterInAny = true;
        if (a.f2 < b.f2) betterInAny = true;
        return betterInAny;
    }

    private void assignCrowdingDistance(List<Solution> front) {
        if (front.isEmpty()) return;
        for (Solution s : front) {
            s.crowdingDistance = 0.0;
        }
        if (front.size() <= 2) {
            for (Solution s : front) {
                s.crowdingDistance = Double.POSITIVE_INFINITY;
            }
            return;
        }

        front.sort(Comparator.comparingDouble(s -> s.f1));
        front.get(0).crowdingDistance = Double.POSITIVE_INFINITY;
        front.get(front.size() - 1).crowdingDistance = Double.POSITIVE_INFINITY;
        double minF1 = front.get(0).f1;
        double maxF1 = front.get(front.size() - 1).f1;
        if (maxF1 > minF1) {
            for (int i = 1; i < front.size() - 1; i++) {
                double d = (front.get(i + 1).f1 - front.get(i - 1).f1) / (maxF1 - minF1);
                front.get(i).crowdingDistance += d;
            }
        }

        front.sort(Comparator.comparingDouble(s -> s.f2));
        front.get(0).crowdingDistance = Double.POSITIVE_INFINITY;
        front.get(front.size() - 1).crowdingDistance = Double.POSITIVE_INFINITY;
        double minF2 = front.get(0).f2;
        double maxF2 = front.get(front.size() - 1).f2;
        if (maxF2 > minF2) {
            for (int i = 1; i < front.size() - 1; i++) {
                double d = (front.get(i + 1).f2 - front.get(i - 1).f2) / (maxF2 - minF2);
                front.get(i).crowdingDistance += d;
            }
        }
    }

    private double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static class Solution {
        final double[] x;
        double f1;
        double f2;
        int rank;
        double crowdingDistance;
        int dominationCount;
        final List<Solution> dominated = new ArrayList<>();

        Solution(int nVars) {
            this.x = new double[nVars];
        }

        Solution copy() {
            Solution s = new Solution(this.x.length);
            System.arraycopy(this.x, 0, s.x, 0, this.x.length);
            s.f1 = this.f1;
            s.f2 = this.f2;
            return s;
        }

        @Override
        public String toString() {
            return String.format(Locale.US, "Solution{f1=%.6f,f2=%.6f,x=%s}", f1, f2, Arrays.toString(x));
        }
    }
}
