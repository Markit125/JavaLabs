package ru.mai.lessons.rpks.impl;

import lombok.extern.slf4j.Slf4j;
import ru.mai.lessons.rpks.IMoneyExchange;
import ru.mai.lessons.rpks.exception.ExchangeIsImpossibleException;

import java.util.*;

public class MoneyExchange implements IMoneyExchange {

    private List<Integer> tryExchange(int sum, List<Integer> coins) {

        if (sum < 0) {
            return null;
        }

        for (int i = 0; i < coins.size(); ++i) {
            if (coins.get(i) <= 0) {
                return null;
            }

            if (sum == coins.get(i)) {
                List<Integer> result = new ArrayList<>();
                result.add(sum);
                return result;
            }


            List<Integer> result = tryExchange(sum - coins.get(i), coins);
            if (result != null) {
                result.add(coins.get(i));
                return result;
            }
        }

        return null;
    }
    public String exchange(Integer sum, String coinDenomination) throws ExchangeIsImpossibleException {

        if (Objects.equals(coinDenomination, "")) {
            throw new ExchangeIsImpossibleException("Empty coin denomination");
        }
        List<Integer> coins = new ArrayList<>();

        for (String s : coinDenomination.split(", ")) {
            coins.add(Integer.parseInt(s));
        }

        coins.sort(Collections.reverseOrder());

        List<Integer> result = tryExchange(sum, coins);

        if (result == null) {
            throw new ExchangeIsImpossibleException("Impossible to exchange");
        }

        StringBuilder answer = new StringBuilder();

        int previous = result.get(result.size() - 1);
        int count = 1;

        for (int i = result.size() - 2; i >= 0; --i) {
            if (previous == result.get(i)) {
                ++count;
                continue;
            } else {
                answer.append(previous).append("[").append(count).append("], ");
                count = 1;
            }
            previous = result.get(i);
        }
        answer.append(previous).append("[").append(count).append("]");

        return answer.toString();
    }
}
