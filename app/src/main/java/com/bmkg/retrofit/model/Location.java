//package com.bmkg.retrofit.model;
//
//public class Location {
//    private String code;
//    private String name;
//
//    public String getCode() {
//        return code;
//    }
//
//    public String getName() {
//        return name;
//    }
//}

package com.bmkg.retrofit.model;

public class Location {
    private String code;
    private String name;

    // --- TAMBAHAN SETTER ---
    public void setCode(String code) {
        this.code = code;
    }

    public void setName(String name) {
        this.name = name;
    }
    // -----------------------

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
