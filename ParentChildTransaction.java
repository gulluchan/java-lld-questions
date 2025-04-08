package org.example;

import java.util.*;

interface Transact {
    int execute(List<TransactionDTO> transactionDTOList, int blockSize);
}

class Person implements Transact {

    @Override
    public int execute(List<TransactionDTO> transactionDTOList, int blockSize) {

        int[] dp = new int[blockSize + 1];
        for (TransactionDTO transactionDTO : transactionDTOList) {
            for (int w = blockSize; w >= transactionDTO.size; w--) {
                dp[w] = Math.max(dp[w], transactionDTO.fee + dp[w - transactionDTO.size]);
            }
        }
        return dp[blockSize];
    }
}

class TransactionDTO {
    public String id;
    public int fee;
    public int size;

    public TransactionDTO(String id, int fee, int size) {
        this.id = id;
        this.fee = fee;
        this.size = size;
    }
}

public class ParentChildTransaction {
    public static void main(String[] args) {
        TransactionDTO dto = new TransactionDTO("1", 1, 1);
        TransactionDTO dto2 = new TransactionDTO("2", 3, 1);
        TransactionDTO dto3 = new TransactionDTO("3", 5, 10);
        List<TransactionDTO> transactionDTOList = new ArrayList<>(List.of(dto, dto2, dto3));
        Person person = new Person();
        int res = person.execute(transactionDTOList, 3);
        System.out.println("result==========" + res);
    }
}
