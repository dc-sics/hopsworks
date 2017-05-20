package io.hops.hopsworks.common.dao.user;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UserDTO {

  private String username;
  private String email;
  private String telephoneNum;
  private String firstName;
  private String lastName;
  private int status;
  private String securityQuestion;
  private String securityAnswer;
  private String secret;
  private String chosenPassword;
  private String repeatedPassword;
  private boolean ToS;
  private boolean twoFactor;
  private int toursState;
  private String orgName;
  private String dep;
  private String street;
  private String city;
  private String postCode;
  private String country;
  private int maxNumProjects;
  private boolean testUser;

  public UserDTO() {
  }

  public UserDTO(Users user) {
    this.username = user.getUsername();
    this.email = user.getEmail();
    this.firstName = user.getFname();
    this.lastName = user.getLname();
    this.telephoneNum = user.getMobile();
    if (user.getOrganization() != null) {
      this.orgName = user.getOrganization().getOrgName();
      this.dep = user.getOrganization().getDepartment();
    }
    if (user.getAddress() != null) {
      this.street = user.getAddress().getAddress2();
      this.city = user.getAddress().getCity();
      this.postCode = user.getAddress().getPostalcode();
      this.country = user.getAddress().getCountry();
    }
    this.maxNumProjects = user.getMaxNumProjects();
    this.twoFactor = user.getTwoFactor();
    this.toursState = user.getToursState();
  }

  public String getUsername() { return username; }

  public void setUsername(String username) { this.username = username; }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getTelephoneNum() {
    return telephoneNum;
  }

  public void setTelephoneNum(String telephoneNum) {
    this.telephoneNum = telephoneNum;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public String getSecurityQuestion() {
    return securityQuestion;
  }

  public void setSecurityQuestion(String securityQuestion) {
    this.securityQuestion = securityQuestion;
  }

  public String getSecurityAnswer() {
    return securityAnswer;
  }

  public void setSecurityAnswer(String securityAnswer) {
    this.securityAnswer = securityAnswer;
  }

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public String getChosenPassword() {
    return chosenPassword;
  }

  public void setChosenPassword(String chosenPassword) {
    this.chosenPassword = chosenPassword;
  }

  public String getRepeatedPassword() {
    return repeatedPassword;
  }

  public void setRepeatedPassword(String repeatedPassword) {
    this.repeatedPassword = repeatedPassword;
  }

  public boolean getToS() {
    return ToS;
  }

  public void setToS(boolean ToS) {
    this.ToS = ToS;
  }

  public boolean isTwoFactor() {
    return twoFactor;
  }

  public void setTwoFactor(boolean twoFactor) {
    this.twoFactor = twoFactor;
  }

  public int getToursState() {
    return toursState;
  }
  
  public void setToursState(int toursState) {
    this.toursState = toursState;
  }
  
  public String getOrgName() {
    return orgName;
  }

  public void setOrgName(String orgName) {
    this.orgName = orgName;
  }

  public String getDep() {
    return dep;
  }

  public void setDep(String dep) {
    this.dep = dep;
  }

  public String getStreet() {
    return street;
  }

  public void setStreet(String street) {
    this.street = street;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getPostCode() {
    return postCode;
  }

  public void setPostCode(String postCode) {
    this.postCode = postCode;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public int getMaxNumProjects() {
    return maxNumProjects;
  }

  public void setMaxNumProjects(int maxNumProjects) {
    this.maxNumProjects = maxNumProjects;
  }

  public boolean isTestUser() {
    return testUser;
  }

  public void setTestUser(boolean testUser) {
    this.testUser = testUser;
  }

  @Override
  public String toString() {
    return "UserDTO{username=" + username + ", email=" + email + ", telephoneNum=" + telephoneNum
            + ", firstName=" + firstName + ", lastName=" + lastName
            + ", status=" + status + ", securityQuestion=" + securityQuestion
            + ", securityAnswer=" + securityAnswer + ", secret=" + secret
            + ", chosenPassword=" + chosenPassword + ", repeatedPassword="
            + repeatedPassword + ", ToS=" + ToS + ", orgName=" + orgName
            + ", dep=" + dep + ", street=" + street + ", city=" + city
            + ", postCode= " + postCode + ", country=" + country
            + ", maxNumProjects= " + maxNumProjects + '}';
  }

}
