# Grocery Store

Mobile-responsive Spring Boot + Thymeleaf grocery application for rice and oil sales.

## Stack

- Java 11
- Spring Boot 2.7.18
- Spring Data JPA
- Spring Security role-based admin access
- Thymeleaf + Bootstrap 5
- MS SQL Server

## Database

The app expects SQL Server at `localhost:1433` with:

- Database: `grocery_store`
- Username: `GROCERY`
- Password: `root`

Update `src/main/resources/application.yml` if your SQL Server host or database name is different.

## Staff Portal

- Username: `admin`
- Password: `admin123`

## Run

```powershell
mvn spring-boot:run
```

Then open:

- Store: `http://localhost:8080/`
- Admin: `http://localhost:8080/admin/dashboard`

Payment screenshots are stored under `uploads/payments`.
