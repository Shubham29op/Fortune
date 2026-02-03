package com.portfolio.backend.service;

import com.portfolio.backend.entity.Client;

import java.util.List;

public interface ClientServiceInterface {
    List<Client> getAllClients();
    Client addClient(Client client);
}
