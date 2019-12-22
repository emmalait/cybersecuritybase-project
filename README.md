# Cyber Security Base 2019-2020: Project I

This is an exercise project for the 2019-2020 edition of University of Helsinki and F-Secure's MOOC [Cyber Security Base](https://cybersecuritybase.mooc.fi/). The instructions for the project were to create a web application that has at least five different flaws from the [OWASP top ten list](https://www.owasp.org/images/7/72/OWASP_Top_10-2017_%28en%29.pdf.pdf). The base of the project is from [here](https://github.com/cybersecuritybase/cybersecuritybase-project). 

## Installation

Clone the repo:

```
git clone 
```

Navigate to repo folder:

```
cd cybersecuritybase-project
```

Create jar:

```
mvn package
```

Run the app:

```
java -jar target/cybersecuritybase-project-1.0-SNAPSHOT.jar
```

The app is now running at [http://localhost:8080](http://localhost:8080).

## Flaw report

* [1: SQL injection](#flaw-1-sql-injection)
* [2: Broken authentication](#flaw-2-broken-authentication)
* [3: Sensitive data exposure](#flaw-3-sensitive-data-exposure)
* [4: Security misconfiguration](#flaw-4-security-misconfiguration)
* [5: Cross-site scripting (XSS)](#flaw-5-cross-site-scripting-xss)

### Flaw 1: SQL injection

**Description**: The input in the name field is used to show the user a list of their signups. The input is not sanitised in any way and the query does not use a parametrizes query, but the input is directly used in the SQL query to fetch the signups. Thus, if one enters e.g. `John' OR 'a' = 'a`, the entire contents of the database are displayed.

**How to fix it**: The whole functionality of fetching signups via the name property is not very secure, as many people can sign up with the same name. If the signup should be shown on the Thanks page, the information could be passed down from the parameters, e.g.:

```java
@RequestMapping(value = "/form", method = RequestMethod.POST)
public String submitForm(@RequestParam String name, @RequestParam String address, Model model) {
    Signup s = new Signup(name, address);
    signupRepository.save(s);
    model.addAttribute("signup", s);
    return "done";
}
```

If, however, the signups should be fetched using the name parameter, the query should be made injection-proof. The program currently uses an additonal custom implementation of the SignupRepository, which uses its own EntityManager to make the insecure SQL query. This is why the injection is possible. This issue would be fixed e.g. by using Spring's native implementation for custome queries. A parametrised `WHERE` query is supported with the `@Query` notation:

```java
@Query("SELECT s FROM Signup s where s.name = ?1") 
String findByName(String name);
```

### Flaw 2: Broken authentication

**Description**: The application requires the user to authenticate in order to access the signup form. Even though the application might look secure in this respect, it breaks a few cardinal rules. First, the application has no password requirements such as minimum length, or that it must contain capital letters, numbers or special characters. The user can choose to have e.g. "1" as their password, which is very unsecure. Allowing users to use weak passwords makes the application and its users vulnerable as the passwords might be easy to guess. Second, the passwords of users are stored as plain text in the database (see flaw 3 below).

**How to fix it**: In order to fix the password minimum requirements, the password input should be validated in AccountController's submitForm method before creating the account e.g. by using regex:

```java
@RequestMapping(value = "/register", method = RequestMethod.POST)
public String submitForm(@RequestParam String username, @RequestParam String password, Model model) {
    String requirement = "((?=.*[a-z])(?=.*\\\\d)(?=.*[A-Z])(?=.*[@#$%!]).{8,20})";
    Boolean passwordOk = password.matches(requirement);
        
    if (passwordOk) {
        accountRepository.save(new Account(username, password));
        return "redirect:/login";
    } else {
        model.addAttribute("error", "Weak password!");
        return "register";
    }    
}
```
The requirement string in the above checks the following criteria: 

* 8-20 characters
* At least 1 digit
* At least 1 lower case character
* At least 1 upper case character
* At least 1 special character from [ @ # $ % ! . ]

In order to show the error to the user, the following should be added to register.html:

```html
<span th:text="${error != null} ? ${error} : ''"></span>
```

This renders the error if there is one, but does not show anything if no errors are passed down to it.

### Flaw 3: Sensitive data exposure

**Description**: The application stores its users' passwords in plain text into the database, which is very dangerous if the database contents were to leak outside the application.

**How to fix it**: Storing passwords in crypted form is possible by using a password encoder. Password encoder is configured in SecurityConfiguration. In order to add a password encoder, define an encoder:

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

In order for the user's password to be encrypted upon registration, the password needs to be hashed before sending it to the database. Change the submitForm method in AccountController to the following:

```java
@RequestMapping(value = "/register", method = RequestMethod.POST)
public String submitForm(@RequestParam String username, @RequestParam String password) {
    Account a = new Account();
    a.setUsername(username);
    a.setPassword(passwordEncoder.encode(password));
    accountRepository.save(a);
    return "redirect:/login";
}
```

Finally, in order for the authentication to also use the encoder, change method configureGlobal in SecurityConfiguration to the following:

```java
@Autowired
public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
    auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
}
```

### Flaw 4: Security misconfiguration

**Description**: The application's current security configuration allows for a CSRF attack, because CSRF checks are disabled. If the user is logged in, a signup could be forced by e.g. rendering an image on another page with a suitable source URL (`http://<app url>/form?name={name}&address={address}`).

**How to fix it**: Delete the following from SecurityConfiguration's configure method:

```java
http.csrf().disable();
http.headers().frameOptions().sameOrigin();
```

As CSRF check is on by default, it is enough to delete the disablement. 

### Flaw 5: Cross-site scripting (XSS)

**Description**: If a user enters a script or any other HTML code (e.g. `<script language="javascript" type="text/javascript">alert("You should not be seeing this");</script>`) to the signup form, the code is not escaped and it is executed when the Thank you page is rendered.

**How to fix it**: The done.html Thymeleaf template currently renders the values using `th:utext`, which does not escape code. By changing them to `th:text`this problem is solved. The end result looks like this:

```html
<ul>
    <li th:each="signup : ${signups}">
        <span th:text="${signup.name}"></span> <span th:text="${signup.address}"></span>
    </li>
</ul>
```