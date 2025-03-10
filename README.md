# Sales Agent Integration
Sales Agent is an Integration for Telex Web App that allows users to find leads and increase interactions. It allows users to set up their criteria and get leads that match those criteria. A Cron job runs every day to query apollo.io API which returns links to leads and stores them in the database. The integration utilizes Mistral AI to draft personalized emails to the leads and send them out. The integration also allows users to track the status of the emails sent out and the responses received.

## Project Structure
The project is structured as follows:
```bash
sales-agent
├── src
│   ├── main
│   │   ├── java
│   │   │   └── integrations
│   │   │       └── telex
│   │   │           └── salesagent
│   │   │               ├── SalesAgentApplication.java
│   │   │               ├── controller
│   │   │               │   └── SalesAgentController.java
│   │   │               ├── service
│   │   │               │   └── SalesAgentService.java
│   │   ├── resources
            └── application.properties
│   └── test
│       ├── java
│       │   └── integrations
│       │       └── telex
│       │           └── salesagent
│       │               ├── controller
│       │               │   └── SalesAgentControllerTest.java
│       │               ├── service
│       │               │   └── SalesAgentServiceTest.java
```

## Getting Started
Ensure you have the following installed on your machine:
* Java 21 or higher
* Maven
* Postgres
* Java IDE (IntelliJ IDEA, Eclipse, etc.)
* Java SDK

## Installation
1. Clone the repository
```bash
git clone https://github.com/telexintegrations/sales-agent.git (Using HTTPS)
or
git clone git@github.com:telexintegrations/sales-agent.git (Using SSH)
```
2. Open the project in your Java IDE
