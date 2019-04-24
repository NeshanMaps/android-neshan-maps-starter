package org.neshan.sample.starter.model.search;


import java.util.List;

public class NeshanSearch {

    private Integer count;
    private List<Item> items ;

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }
}
