# Security Policy
Supported Versions
We actively provide security patches for the following versions of our platform. We recommend all users stay on the latest stable release to ensure the highest level of protection.

Version,Supported,Notes
1.1.x,:white_check_mark:,Current Stable Release (Enhancement Branch)
1.0.x,:white_check_mark:,Maintenance Support (Critical Patches Only)
< 1.0,:x:,End of Life (EOL)

Reporting a Vulnerability
We take the security of our data and our clients' portfolios very seriously. If you believe you have found a security vulnerability, please follow the process below:

1. How to Report
Do not open a public GitHub issue for security vulnerabilities. Instead, please send a detailed report to:

Email: fortune@gmail.com

Subject: [Vulnerability Report] - <Brief Description>

2. What to Include
To help us triage and fix the issue quickly, please include:

A description of the vulnerability and its potential impact.

Steps to reproduce (Proof of Concept).

Any code or logs that demonstrate the issue.

The version of the application you were using.

3. What to Expect
Response Time: You will receive an initial acknowledgment of your report within 24-48 hours.

Updates: We will provide status updates at least once every 5 business days until the issue is resolved.

Disclosure: Once a fix is deployed, we will coordinate a public disclosure date with you. We ask that you maintain confidentiality until the patch is live to protect our users.

Our Commitment to Security
Gemini API Safety: We utilize Google’s built-in safety filters to prevent the generation of harmful content.

Data Masking: Sensitive portfolio data is processed via our buildContext() service with strict PII (Personally Identifiable Information) filtering before being sent to the LLM.

Code Scanning: We perform regular static analysis on our Spring Boot backend to catch common vulnerabilities  early.
