package $package;

/**
 * @author <a href="mailto:dmitriy.g.matveev@gmail.com">Dmitry Matveev</a>
 */
public $classKind $className {
    #foreach($method in $methods)
        $method.flags $method.returnType $method.name (
                #foreach($parameterType in $method.parameterTypes)
                    $parameterType arg$foreach.index #if($foreach.hasNext ),#end
                #end
        );
    #end
}
