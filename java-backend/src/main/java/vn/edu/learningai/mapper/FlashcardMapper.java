package vn.edu.learningai.mapper;

import org.mapstruct.Mapper;
import vn.edu.learningai.dto.response.CardResponse;
import vn.edu.learningai.entity.Flashcard;

@Mapper(componentModel = "spring")
public interface FlashcardMapper {

    CardResponse toCardResponse(Flashcard flashcard);
}
