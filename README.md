# Sales Agent AI

## Overview

Sales Agent Integration introduces a cutting-edge solution for business owners to discover potential leads with ease. By specifying their business type and target audience through a simple chat with a sales agent, users receive a curated list of relevant leads in an organized format. This streamlined process enhances customer outreach and supports business growth.

## Key Features

- **Chat-Based Input**: Business owners can provide their business type and target audience through a friendly chat interface with the sales agent on the telex platform.
- **Curated Lead Generation**: The sales agent retrieves a list of potential leads tailored to the specified criteria.
- **Organized Output**: Leads are presented in a clear and easy-to-read format.
- **Custom Automation**: The lead retrieval process runs automatically at a set time, ensuring business owners always have access to updated leads.
- **Telex Success Notifications**: Users receive success messages via the Telex channel whenever new leads are fetched.


## How It Works
### Telex Setup and activating the Sales Agent
#### Steps
1. **Sign Up**: Sign up for a Telex account at [Telex](https://telex.im).
2. **Create an Organisation**: Create an organisation on the Telex platform.
   ![Step 1: Create Organisation](src/main/resources/static/telex-setup/1.png)
3. **Create a Channel**: Create a channel in the organisation.
   ![Step 2: Create Channel](src/main/resources/static/telex-setup/2.png)
4. **Add the Sales Agent**: Click on `Apps`, then click on `Add New` to add the Sales Agent to the channel.
![Step 3: Add Sales Agent Integration](src/main/resources/static/telex-setup/3.png)
5. **Sales Agent Integration**: Enter this URL `https://sales-agent-3wyf.onrender.com/integration.json` in the `Integration Json Url` field and click `Save`.
6. **Activate the Sales Agent**: Click on `the toggle button` to activate the Sales Agent in the channel.
7. **Confugure the Sales Agent**: Click on the `Manage App` button to configure the Sales Agent.
8. **Set the Custom Channel**: Click on the `Output` tab -> Custom Channels -> Add Custom Channel -> Select the channel you want to see the output.
![Step 4: Activate Sales Agent](src/main/resources/static/telex-setup/4.png)
9. You're all set! The Sales Agent is now active in the channel.

### Sales Agent Chat Interaction

The chat interaction between business owners and the sales agent takes place in the telex channel and is handled by the `ChatService` class  which processes messages, validates inputs, and guides users through a structured flow to collect information for lead generation. Below is a breakdown of the interaction steps:

#### Interaction Flow

1. **Starting the Process**:
    - Users begin the process by sending the `start-sales-agent` command.
    - The system responds with a welcome message and prompts users to provide their business email address.

       ![Step 1: Starting the Process](src/main/resources/static/sales-agent/1.png)

2. **Exiting the Process**:
    - At any point during the interaction, users can exit the process by sending the `/exit` command.
    - The system acknowledges the exit request and stops the interaction.

       ![Step 2: Exiting the Process](src/main/resources/static/sales-agent/6.png)

3. **Email Validation**:
    - The system validates the provided email address using a regex pattern.
      ![Step 2: Accepted Email](src/main/resources/static/sales-agent/3.png)
    - If the email is invalid or already exists in the system, an appropriate message is displayed.
   
      ![Step 3: Email Validation](src/main/resources/static/sales-agent/2.png)
4. **Company Information**:
    - Users are prompted to specify the platform they are targeting to get leads. The Agent can search for leads from any of the following platforms `linkedin`, `x (formerly twitter)`, `stripe`, `google`.
    - Input is validated to ensure it follows the required format.

      ![Step 3: Company Information](src/main/resources/static/sales-agent/4.png)

5. **Lead Type**:
    - Users specify the type of lead they need by providing the lead's domain name (e.g., `linkedin.com`, `x.com`, `google.com`, `stripe.com`).

      ![Step 4: Lead Type](src/main/resources/static/sales-agent/5.png)

6. **Automated Instructions**:
    - Clear and friendly instructions are sent at every step of the process.
    - The system communicates with users via the configured Telex webhook URL.

7. **Persisting User Data**:
    - Once all required inputs are collected, the user's information (email, company name, and lead type) is saved in the database for further processing.

   
8. **Domain Search Integration**:
    - The service triggers the `domain-search` endpoint of the lead generation API to fetch leads matching the specified criteria.

9. **Success Notification**: 
    - Upon successful lead retrieval, the system sends a notification to the user via the Telex channel with all the leads found.

      ![Step 8: Leads Found](src/main/resources/static/sales-agent/new-lead-1.png)
      ![Step 8: Leads Found](src/main/resources/static/sales-agent/new-lead-2.png)
      ![Step 8: Leads Found](src/main/resources/static/sales-agent/new-lead-3.png)
   ![Step 8: Leads Found](src/main/resources/static/sales-agent/new-lead-4.png)
      ![Step 8: Leads Found](src/main/resources/static/sales-agent/new-lead-5.png)
      ![Step 8: Leads Found](src/main/resources/static/sales-agent/new-lead-6.png)

### Lead Search Integration

The lead search feature is implemented in the `domainSearch` method of the `LeadService` class. It retrieves potential leads based on the user's specified domain and stores new, unique leads in the database. Below is an overview of its workflow:

#### Workflow

1. **Retrieve User Details**:
   - The `domainSearch` method starts by fetching the user's information from the database using the `channel_id` from the `SalesAgentPayloadDTO` payload.
   - If the user is not found, an exception is thrown.

2. **Construct API Request**:
   - The method uses the domain provided by the user (`LeadType`) to construct the `domain-search` API request.
   - The API key and base URL for the request are configured in `OkHttpConfig`.

3. **Fetch and Parse Data**:
   - A `GET` request is sent to the Hunter.io API
   - The API response is parsed into a JSON object to extract relevant data, such as email addresses, names, LinkedIn URLs, company information, and industry.

4. **Filter Unique Leads**:
   - Existing leads in the database are checked to ensure no duplicates are stored.
   - New leads are created only for entries with unique email addresses.

5. **Save Leads and Notify**:
   - New leads are saved to the database.
   - For each new lead, the system sends a notification to the specified `Telex channel` using the `TelexClient`.

6. **Log Success**:
   - Logs are generated to confirm how many new leads were successfully saved.
