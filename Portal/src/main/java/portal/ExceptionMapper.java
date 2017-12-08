package portal;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class ExceptionMapper implements javax.ws.rs.ext.ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable exception)
    {
        exception.printStackTrace();
        return Response.status(getStatusCode(exception))
                .build();
    }

    /*
     * Get appropriate HTTP status code for an exception.
     */
    private int getStatusCode(Throwable exception)
    {
        if (exception instanceof WebApplicationException)
        {
            return ((WebApplicationException)exception).getResponse().getStatus();
        }

        return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    }

}
