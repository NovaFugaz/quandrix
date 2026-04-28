# Quandrix - Yet Another MtG singles store.

<!-- ABOUT THE PROJECT -->
## About The Project

Quandrix is just what it is. A backend for a Magic The Gathering singles store.
The microservices being handled are described as follow:
    - ms-auth: Authentication and Security
    - ms-users: User login and stuff.
        There's three type of users:
        - Persona: Any person willing to buy.
        - Tienda: Any store employee that sells stuff.
        - Admin: Admins of the store/project.
    - ms-catalog: Catalogue of the cards, pulled from fetching Scryfall API.
    - ms-listings: Listings, published by the store.
    - ms-orders: Orders on buying cards.
    - ms-payments: For anything payment related. Mockup mostly due to integrations with APIs.
    - ms-notifications: On transaction being done, it sends a notification to the user.
    - ms-reports: Reports done by admins on how has been the store performing.
    - ms-reviews: Reviews on the transactions being done.
    - ms-transactions: Transient package for transactions management.
Its purpose is to be a project for FullStack Dev I at uni.
Any other documentation or stuff will be added later.

### Built With

- Java 21
- Maven
- Spring Boot

## Roadmap

- [x] Add microservices structure
- [x] Add databases
- [ ] Working microservices (on it!)
    Working:
    - ms-auth
    In Progress:
    - ms-users
    To be worked:
    - the rest.
- [ ] Testing

## License

Distributed under GPL2.

## Contact

- Ignacia Padilla - svt.nova@pm.me
- Elba Sánchez - elb.sanchezs@duocuc.cl

