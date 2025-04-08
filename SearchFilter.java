package org.example;

import java.util.*;

interface Filter {
    Boolean solve(Transaction transaction);
}

class UserIdFilter implements Filter {
    String userId;

    public UserIdFilter(String userId) {
        this.userId = userId;
    }


    @Override
    public Boolean solve(Transaction transaction) {
        return Objects.equals(userId, transaction.userId);
    }
}

class AmountFilter implements Filter {
    Double amount;

    public AmountFilter(Double amount) {
        this.amount = amount;
    }


    @Override
    public Boolean solve(Transaction transaction) {
        return transaction.amount >= amount;
    }
}

class TimeRangeFilter implements Filter {
    Integer startTime;
    Integer endTime;

    public TimeRangeFilter(Integer startTime, Integer endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }


    @Override
    public Boolean solve(Transaction transaction) {
        return transaction.timestamp >= startTime && transaction.timestamp <= endTime;
    }
}

class Transaction {
    public String id;
    public String userId;
    public Integer timestamp;
    public Double amount;
    public String cursorIndex;

    public Transaction(String id, String userId, Integer timestamp, Double amount) {
        this.id = id;
        this.userId = userId;
        this.timestamp = timestamp;
        this.amount = amount;
    }

    public String getCursorIndex() {
        return id + "_" + timestamp;
    }
}

class SearchRequest {
    public List<Filter> filters;
    public int pageSize;
    public String lastCursor;

    public SearchRequest(List<Filter> filters, int pageSize, String lastCursor) {
        this.filters = filters;
        this.pageSize = pageSize;
        this.lastCursor = lastCursor;
    }
}

class SearchResponse {
    public List<Transaction> transactions;
    public String lastCursor;

    public SearchResponse(List<Transaction> transactions, String lastCursor) {
        this.transactions = transactions;
        this.lastCursor = lastCursor;
    }

    public SearchResponse() {
        transactions = new ArrayList<>();
        lastCursor = null;
    }

}

class Database {
    private final List<Transaction> transactions;

    public Database(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    SearchResponse search(SearchRequest searchRequest) {
        List<Transaction> res = new ArrayList<>();
        String cursor = null;
        boolean cursorMatch = searchRequest.lastCursor == null;
        for (Transaction transaction : transactions) {
            boolean valid = true;
            if (!cursorMatch) {
                if (!searchRequest.lastCursor.equals(transaction.getCursorIndex())) {
                    continue;
                }
                cursorMatch = true;
                continue;
            }
//            for (Filter filter : searchRequest.filters) {
//                if (!filter.solve(transaction)) {
//                    valid = false;
//                    break;
//                }
//            }
            if (valid) {
                res.add(transaction);
            }

            if (res.size() == searchRequest.pageSize) {
                cursor = transaction.getCursorIndex();
                break;
            }
        }
        return new SearchResponse(res, cursor);
    }
};

public class SearchFilter {
    public static void main(String[] args) {
        Transaction t1 = new Transaction("1", "11", 1234, 23.0);
        Transaction t2 = new Transaction("2", "11", 1236, 35.0);
        Transaction t3 = new Transaction("3", "13", 1230, 40.0);
        Transaction t4 = new Transaction("4", "13", 1210, 10.0);
        List<Transaction> transactions = new ArrayList<>(Arrays.asList(t1, t2, t3, t4));

        AmountFilter amountFilter = new AmountFilter(10.0);
        TimeRangeFilter timeRangeFilter = new TimeRangeFilter(1200, 1240);
        UserIdFilter userIdFilter = new UserIdFilter("11");
        Database database = new Database(transactions);
        SearchRequest searchRequest = new SearchRequest(List.of(amountFilter, timeRangeFilter, userIdFilter), 2, null);
        SearchResponse results = database.search(searchRequest);
        for (Transaction t : results.transactions) {
            System.out.println(t.id + " " + t.userId + " " + t.timestamp + " " + t.amount);

        }
        System.out.println("-------------");
        while (results.lastCursor != null) {
            SearchRequest nextSearchRequest = new SearchRequest(List.of(amountFilter, timeRangeFilter, userIdFilter), 2, results.lastCursor);
            results = database.search(nextSearchRequest);
            if(results.lastCursor == null) {
                break;
            }
            for (Transaction t : results.transactions) {
                System.out.println(t.id + " " + t.userId + " " + t.timestamp + " " + t.amount);
            }
            System.out.println("------------");
        }
    }
}


/*



 */
