package com.example.mvpapp;

class Site {
    private String siteName;
    private Double siteLat;
    private Double siteLong;

    Site(String siteName) {
        this.siteName = siteName;
    }

    public String getSiteName() {
        return siteName;
    }

    public Double getSiteLat() {
        return siteLat;
    }

    public Double getSiteLong() {
        return siteLong;
    }

    public void setSiteLat(Double siteLat) {
        this.siteLat = siteLat;
    }

    public void setSiteLong(Double siteLong) {
        this.siteLong = siteLong;
    }
}
