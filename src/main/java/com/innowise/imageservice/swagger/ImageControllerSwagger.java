package com.innowise.imageservice.swagger;

import com.innowise.imageservice.dto.CommentRequestDto;
import com.innowise.imageservice.dto.CommentResponseDto;
import com.innowise.imageservice.dto.ErrorResponse;
import com.innowise.imageservice.dto.ImageRequestDto;
import com.innowise.imageservice.dto.ImageResponseDto;
import com.innowise.imageservice.dto.PaginatedSliceResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@Tag(name = "image", description = "Image management and interaction endpoints")
@SecurityRequirement(name = "JWT")
public interface ImageControllerSwagger {

    @Operation(
            summary = "Upload a new image",
            description = """
                    Uploads a new image to the system with associated metadata.
                    The request must include a multipart form with an `imageRequest` JSON object and a `file`
                    part containing the image.
                    Example `imageRequest`:
                    ```json
                    {
                        "description": "A beautiful sunset"
                    }
                    ```
                    Requirements:
                    - `X-User-Id` header: Must not be blank, automatically provided.
                    - `imageRequest.description`: Optional, maximum 255 characters.
                    - `file`: Must be a valid image (JPEG, PNG, BMP, or WebP), size between configured min and max bytes.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Image uploaded successfully",
                    content = @Content(schema = @Schema(implementation = ImageResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data, image size, or type",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(path = "/images", consumes = MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ImageResponseDto> upload(
            @Parameter(hidden = true)
            @RequestHeader("X-User-Id")
            String userId,
            @Parameter(schema =@Schema(type = "string", format = "binary"),
                    description = "Image metadata (description)", required = true)
            @RequestPart("imageRequest") ImageRequestDto imageRequestDto,
            @Parameter(description = "Image file (JPEG, PNG, BMP, or WebP)", required = true)
            @RequestPart("file") MultipartFile imageFile
    );

    @Operation(
            summary = "Get image by ID",
            description = """
                    Retrieves an image by its ID.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ImageResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Image not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/images/{id}")
    ResponseEntity<ImageResponseDto> getById(
            @Parameter(description = "ID of the image to retrieve", required = true)
            @PathVariable("id") Long imageId
    );

    @Operation(
            summary = "Get all images by user ID",
            description = """
                    Retrieves a paginated list of images uploaded by the authenticated user.
                    Default page is 0, default size is 20.
                    The `X-User-Id` header is automatically provided.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Images retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PaginatedSliceResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/users/images")
    ResponseEntity<PaginatedSliceResponseDto<ImageResponseDto>> getAllByUserId(
            @Parameter(hidden = true)
            @RequestHeader("X-User-Id") String currentUserId,
            @Parameter(description = "Page number (default: 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default: 20)")
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(
            summary = "Get all images",
            description = """
                    Retrieves a paginated list of all images in the system.
                    Default page is 0, default size is 20.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Images retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PaginatedSliceResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/images")
    ResponseEntity<PaginatedSliceResponseDto<ImageResponseDto>> getAll(
            @Parameter(description = "Page number (default: 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default: 20)")
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(
            summary = "Like or unlike an image",
            description = """
                    Toggles a like on an image. If the user has not liked the image, a like is added;
                    if already liked, the like is removed.
                    Returns "Liked" or "Disliked" as a string.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Like status updated successfully",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "Image not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/images/{id}/likes")
    ResponseEntity<String> pressOrDeleteLike(
            @Parameter(hidden = true)
            @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "ID of the image to like/unlike", required = true)
            @PathVariable("id") Long imageId
    );

    @Operation(
            summary = "Add a comment to an image",
            description = """
                    Adds a comment to an image.
                    Example `commentRequestDto`:
                    ```json
                    {
                        "content": "Great photo!"
                    }
                    ```
                    Requirements:
                    - `content`: Must not be blank, maximum 300 characters.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Comment added successfully",
                    content = @Content(schema = @Schema(implementation = CommentResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid comment data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Image not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/images/{id}/comments")
    ResponseEntity<CommentResponseDto> addComment(
            @Parameter(hidden = true)
            @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "ID of the image to comment on", required = true)
            @PathVariable("id") Long imageId,
            @Parameter(description = "Comment details (content)", required = true)
            @Valid @RequestBody CommentRequestDto commentRequestDto
    );

    @Operation(
            summary = "Get all comments for an image",
            description = """
                    Retrieves a paginated list of comments for a specific image.
                    Default page is 0, default size is 5.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comments retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PaginatedSliceResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Image not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/images/{id}/comments")
    ResponseEntity<PaginatedSliceResponseDto<CommentResponseDto>> getAllCommentsByImageId(
            @Parameter(description = "ID of the image to retrieve comments for", required = true)
            @PathVariable("id") Long imageId,
            @Parameter(description = "Page number (default: 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default: 5)")
            @RequestParam(defaultValue = "5") int size
    );

    @Operation(
            summary = "Delete a comment",
            description = """
                    Deletes a comment from an image. Only the comment owner or the image owner can delete the comment.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Comment deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Operation not allowed (not comment or image owner)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Image or comment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/images/{id}/comments/{commentId}")
    ResponseEntity<?> deleteComment(
            @Parameter(hidden = true)
            @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "ID of the image", required = true)
            @PathVariable("id") Long imageId,
            @Parameter(description = "ID of the comment to delete", required = true)
            @PathVariable("commentId") Long commentId
    );

    @Operation(
            summary = "Update a comment",
            description = """
                    Updates a comment on an image. Only the comment owner can update the comment.
                    Example `commentRequestDto`:
                    ```json
                    {
                        "content": "Updated comment text"
                    }
                    ```
                    Requirements:
                    - `content`: Must not be blank, maximum 300 characters.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comment updated successfully",
                    content = @Content(schema = @Schema(implementation = CommentResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid comment data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Operation not allowed (not comment owner)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Image or comment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/images/{id}/comments/{commentId}")
    ResponseEntity<CommentResponseDto> updateComment(
            @Parameter(hidden = true)
            @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "ID of the image", required = true)
            @PathVariable("id") Long imageId,
            @Parameter(description = "ID of the comment to update", required = true)
            @PathVariable("commentId") Long commentId,
            @Parameter(description = "Updated comment details (content)", required = true)
            @Valid @RequestBody CommentRequestDto commentRequestDto
    );
}