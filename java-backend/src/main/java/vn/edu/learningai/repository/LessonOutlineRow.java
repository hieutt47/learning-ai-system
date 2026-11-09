package vn.edu.learningai.repository;

/**
 * Projection for the public course outline.
 *
 * <p>Only these three columns are selected, which keeps {@code video_url} and
 * {@code transcript_text} out of the object graph entirely: there is no entity
 * and no field for a guest response to leak by accident.</p>
 */
public record LessonOutlineRow(Long id, String title, Integer orderIndex) {
}
