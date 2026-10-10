package com.funnight.backend.game;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameCatalogController {

    /** Lists all six games and whether they can be played yet. */
    @GetMapping
    public List<GameInfo> listGames() {
        return Arrays.stream(GameType.values()).map(GameInfo::of).toList();
    }
}
