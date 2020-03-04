package us.magicalash.weasel.plugin;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import us.magicalash.weasel.plugin.docparser.JavaDocumentationParser;

public class CommentIgnoringBailStrategy extends DefaultErrorStrategy implements ANTLRErrorStrategy {

    @Override
    protected void reportFailedPredicate(Parser recognizer, FailedPredicateException e) {
        matchCommentOrCancel(recognizer, e);
    }

    @Override
    protected void reportInputMismatch(Parser recognizer, InputMismatchException e) {
        matchCommentOrCancel(recognizer, e);
    }

    @Override
    protected void reportNoViableAlternative(Parser recognizer, NoViableAltException e) {
        matchCommentOrCancel(recognizer, e);
    }

    private void matchCommentOrCancel(Parser recognizer, RecognitionException e) {
        if (e.getOffendingToken().getType() != JavaDocumentationParser.JAVADOC_COMMENT) {
            throw new ParseCancellationException(e);
        }
    }
}
