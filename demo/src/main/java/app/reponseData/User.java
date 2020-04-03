package app.reponseData;

public class User {
    private String id;
    private String name;
    private String tel;
    private int age;
    private String email;
    private String address;
    private String url;
    public User(String id, String name, String tel, int age, String email, String address,String url) {
        this.id = id;
        this.name = name;
        this.tel = tel;
        this.age = age;
        this.email = email;
        this.address = address;
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
