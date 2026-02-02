package com.portfolio.backend.controller;

import com.portfolio.backend.entity.Client;
import com.portfolio.backend.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@CrossOrigin(origins = "*") // Allows the Frontend to communicate with this
public class ClientController {

    @Autowired
    private ClientService clientService;

    // GET /api/clients
    // Returns a list of all clients (used for the Manager's dropdown)
    @GetMapping
    public List<Client> getAllClients() {
        return clientService.getAllClients();
    }

    // POST /api/clients
    // Creates a new client
    @PostMapping
    public Client createClient(@RequestBody Client client) {
        return clientService.addClient(client);
    }
}