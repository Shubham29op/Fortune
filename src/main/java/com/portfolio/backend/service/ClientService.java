package com.portfolio.backend.service;

import com.portfolio.backend.entity.Client;
import com.portfolio.backend.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    // Retrieve all clients from the database
    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    // Save a new client to the database
    public Client addClient(Client client) {
        return clientRepository.save(client);
    }
}